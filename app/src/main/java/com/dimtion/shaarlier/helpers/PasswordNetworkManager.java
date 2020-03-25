package com.dimtion.shaarlier.helpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.dimtion.shaarlier.utils.Link;
import com.dimtion.shaarlier.utils.ShaarliAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by dimtion on 05/04/2015.
 * This class handle all the communications with Shaarli and other web services
 */
public class PasswordNetworkManager implements NetworkManager {

    private final String mShaarliUrl;
    private final String mUsername;
    private final String mPassword;
    private final boolean mValidateCert;
    private final String mBasicAuth;

    private Map<String, String> mCookies;
    private String mToken;

    private String mDatePostLink;
    private String mSharedUrl;
    private Exception mLastError;

    private String mPrefetchedTitle;
    private String mPrefetchedDescription;
    private String mPrefetchedTags;

    PasswordNetworkManager(@NonNull ShaarliAccount account) {
        this.mShaarliUrl = account.getUrlShaarli();
        this.mUsername = account.getUsername();
        this.mPassword = account.getPassword();
        this.mValidateCert = account.isValidateCert();

        if (!"".equals(account.getBasicAuthUsername()) && !"".equals(account.getBasicAuthPassword())) {
            String login = account.getBasicAuthUsername() + ":" + account.getBasicAuthPassword();
            this.mBasicAuth = new String(Base64.encode(login.getBytes(), Base64.NO_WRAP));
        } else {
            this.mBasicAuth = "";
        }
    }

    /**
     * Helper method which create a new connection to Shaarli
     *
     * @param url    the url of the shaarli
     * @param method set the HTTP method to use
     * @return pre-made jsoupConnection
     */
    private Connection newConnection(String url, Connection.Method method) {
        Connection jsoupConnection = Jsoup.connect(url);

        if (!"".equals(this.mBasicAuth)) {
            jsoupConnection = jsoupConnection.header("Authorization", "Basic " + this.mBasicAuth);
        }
        if (this.mCookies != null) {
            jsoupConnection = jsoupConnection.cookies(this.mCookies);
        }

        return jsoupConnection
                .validateTLSCertificates(this.mValidateCert)
                .timeout(NetworkUtils.TIME_OUT)
                .followRedirects(true)
                .method(method);
    }

    @Override
    public boolean isCompatibleShaarli() throws IOException {
        final String loginFormUrl = this.mShaarliUrl + "?do=login";
        try {
            Connection.Response loginFormPage = this.newConnection(loginFormUrl, Connection.Method.GET).execute();
            this.mCookies = loginFormPage.cookies();
            this.mToken = loginFormPage.parse().body().select("input[name=token]").first().attr("value");
        } catch (NullPointerException | IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Method which retrieve the cookie saying that we are logged in
     */
    @Override
    public boolean login() throws IOException {
        try {
            Connection.Response loginPage = this.newConnection(this.mShaarliUrl, Connection.Method.POST)
                    .data("login", this.mUsername)
                    .data("password", this.mPassword)
                    .data("token", this.mToken)
                    .data("returnurl", this.mShaarliUrl)
                    .execute();

            this.mCookies = loginPage.cookies();
            loginPage.parse().body().select("a[href=?do=logout]").first()
                    .attr("href"); // If this fails, you're not connected

        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    /**
     * Method which retrieve a token for posting links
     * Update the cookie, the token and the date
     * Assume being logged in
     */
    private void retrievePostLinkToken(String encodedSharedLink) throws IOException {
        final String postFormUrl = this.mShaarliUrl + "?post=" + encodedSharedLink;

        Connection.Response postFormPage = this.newConnection(postFormUrl, Connection.Method.GET)
                .execute();
        final Element postFormBody = postFormPage.parse().body();

        // Update our situation:
        // TODO: Soft fail: if one field does not load, try the others anyway
        mToken = postFormBody.select("input[name=token]").first().attr("value");
        mDatePostLink = postFormBody.select("input[name=lf_linkdate]").first().attr("value");  // Date chosen by the server
        mSharedUrl = postFormBody.select("input[name=lf_url]").first().attr("value");
        mPrefetchedTitle = postFormBody.select("input[name=lf_title]").first().attr("value");
        mPrefetchedDescription = postFormBody.select("textarea[name=lf_description]").first().html();
        mPrefetchedTags = postFormBody.select("input[name=lf_tags]").first().attr("value");
    }

    @Override
    public Link prefetchLinkData(Link link) throws IOException {
        String encodedShareUrl = URLEncoder.encode(link.getUrl(), "UTF-8");
        retrievePostLinkToken(encodedShareUrl);

        Link newLink = new Link(link);
        newLink.setUrl(mSharedUrl);
        newLink.setTitle(mPrefetchedTitle);
        newLink.setDescription(mPrefetchedDescription);
        newLink.setTags(mPrefetchedTags);
        newLink.setDatePostLink(mDatePostLink);
        newLink.setToken(mToken);
        return newLink;
    }

    /**
     * Method which publishes a link to shaarli
     * Assume being logged in
     * TODO: use the prefetch function
     */
    @Override
    public void pushLink(Link link) throws IOException {
        String encodedShareUrl = URLEncoder.encode(link.getUrl(), "UTF-8");
        retrievePostLinkToken(encodedShareUrl);

        if (NetworkUtils.isUrl(link.getUrl())) { // In case the url isn't really one, just post the one chosen by the server.
            this.mSharedUrl = link.getUrl();
        }

        final String postUrl = this.mShaarliUrl + "?post=" + encodedShareUrl;

        Connection postPageConn = this.newConnection(postUrl, Connection.Method.POST)
                .data("save_edit", "Save")
                .data("token", this.mToken)
                .data("lf_tags", link.getTags())
                .data("lf_linkdate", this.mDatePostLink)
                .data("lf_url", this.mSharedUrl)
                .data("lf_title", link.getTitle())
                .data("lf_description", link.getDescription());
        if (link.isPrivate()) postPageConn.data("lf_private", "on");
        if (link.isTweet()) postPageConn.data("tweet", "on");
        if (link.isToot()) postPageConn.data("toot", "on");
        postPageConn.execute(); // Then we post
    }

    @Override
    public List<String> retrieveTags() {
        List<String> tags = new ArrayList<>();
        try {
            String[] awesompleteTags = this.retrieveTagsFromAwesomplete();
            Collections.addAll(tags, awesompleteTags);
        } catch (Exception e) {
            Log.w("TAG", e.toString());
        }
        try {
            String[] wsTags = this.retrieveTagsFromWs(); // kept for compatibility with old Shaarli instances
            Collections.addAll(tags, wsTags);
        } catch (Exception e) {
            Log.w("TAG", e.toString());
        }
        return tags;
    }

    @Override
    public List<Link> getLinks(@Nullable Integer offset, @Nullable Integer limit) {
        // FIXME: This Network manager cannot download links
        return null;
    }

    /**
     * Method which retrieve tags from the WS (old shaarli)
     * Assume being logged in
     */
    private String[] retrieveTagsFromWs() throws IOException, JSONException {
        final String requestUrl = this.mShaarliUrl + "?ws=tags&term=+";
        String[] predictionsArr = {};
        String json = this.newConnection(requestUrl, Connection.Method.POST)
                .ignoreContentType(true)
                .execute()
                .body();

        JSONArray ja = new JSONArray(json);
        predictionsArr = new String[ja.length()];
        for (int i = 0; i < ja.length(); i++) {
            // add each entry to our array
            predictionsArr[i] = ja.getString(i);
        }

        return predictionsArr;
    }

    /**
     * Method which retrieve tags from awesomplete (new shaarli)
     * Assume being logged in
     */
    private String[] retrieveTagsFromAwesomplete() throws IOException {
        final String requestUrl = this.mShaarliUrl + "?post=";
        String tagsString = this.newConnection(requestUrl, Connection.Method.GET)
                .execute()
                .parse()
                .body()
                .select("input[name=lf_tags]")
                .first()
                .attr("data-list");
        return tagsString.split(", ");
    }
}
