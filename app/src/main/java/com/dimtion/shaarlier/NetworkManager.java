package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.URLUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by dimtion on 05/04/2015.
 * This class handle all the communications with shaarli and other web services
 */
class NetworkManager {
    private final String m_shaarliUrl;
    private final String m_username;
    private final String m_password;
    private Integer m_timeout = 10000;


    //    private Activity m_parentActivity;
    private Map<String, String> m_cookies;
    private String m_token;

    private String m_datePostLink;
    private String m_sharedUrl;

    NetworkManager(String shaarliUrl, String username, String password) {
        this.m_shaarliUrl = shaarliUrl;
        this.m_username = username;
        this.m_password = password;
    }

    //
    // Check if a string is an url
    // TODO : unit test on this, I'm not quite sure it is perfect...
    //
    public static boolean isUrl(String url) {
        return URLUtil.isValidUrl(url) && !url.equals("http://");
    }

    //
    // Change something which is close to a url to something that is really one
    //
    public static String toUrl(String givenUrl) {

        String protocol = "http://";  // Default value
        if (!givenUrl.equals("")) {

            if (!givenUrl.endsWith("/")) {
                givenUrl += '/';
            }

            if (givenUrl.startsWith("http://")) {
                givenUrl = givenUrl.replace("http://", "");

            } else if (givenUrl.startsWith("https://")) {
                givenUrl = givenUrl.replace("https://", "");
                protocol = "https://";
            }
        }

        return protocol + givenUrl;
    }

    //
    // Method to test the network connection (lighter version)
    //
    public static boolean testNetwork(Activity parentActivity) {
        ConnectivityManager connMgr = (ConnectivityManager) parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    //
    // Static method to load the title of a web page
    //
    public static String loadTitle(String url) {
        try {
            Connection.Response pageResp = Jsoup.connect(url)
                    .maxBodySize(50240) // Hopefully we won't need more data
                    .followRedirects(true)
                    .execute();
            return pageResp.parse().title();
        } catch (Exception e) {
            return "";
        }
    }

    //
    // Set the default timeout
    //
    public void setTimeout(int timeout) {
        this.m_timeout = timeout;
    }

    //
    // Check the website is a compatible shaarli, by downloading a token
    //
    public boolean retrieveLoginToken() throws IOException {
        final String loginFormUrl = this.m_shaarliUrl + "?do=login";
        try {
            Connection.Response loginFormPage = Jsoup.connect(loginFormUrl)
                    .timeout(this.m_timeout)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .execute();
            this.m_cookies = loginFormPage.cookies();
            this.m_token = loginFormPage.parse().body().select("input[name=token]").first().attr("value");

        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    //
    // Method which retrieve the cookie saying that we are logged in
    // (login in)
    //
    public boolean login() throws IOException {
        final String loginUrl = this.m_shaarliUrl;
        try {
            Connection.Response loginPage = Jsoup.connect(loginUrl)
                    .method(Connection.Method.POST)
                    .timeout(this.m_timeout)
                    .followRedirects(true)
                    .cookies(this.m_cookies)
                    .data("login", this.m_username)
                    .data("password", this.m_password)
                    .data("token", this.m_token)
                    .data("returnurl", this.m_shaarliUrl)
                    .execute();

            this.m_cookies = loginPage.cookies();
            loginPage.parse().body().select("a[href=?do=logout]").first()
                    .attr("href"); // If this fails, you're not connected

        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    //
    // Method which retrieve a token for posting links
    // Update the cookie, the token and the date
    // Assume being logged in
    //
    void retrievePostLinkToken(String encodedSharedLink) throws IOException {
        final String postFormUrl = this.m_shaarliUrl + "?post=" + encodedSharedLink;
        Connection.Response postFormPage = Jsoup.connect(postFormUrl)
                .followRedirects(true)
                .timeout(m_timeout)
                .cookies(this.m_cookies)
                .timeout(this.m_timeout)
                .execute();
        final Element postFormBody = postFormPage.parse().body();

        // Update our situation :
        this.m_token = postFormBody.select("input[name=token]").first().attr("value");
        this.m_datePostLink = postFormBody.select("input[name=lf_linkdate]").first().attr("value"); // Date choosen by the server
        this.m_sharedUrl = postFormBody.select("input[name=lf_url]").first().attr("value");
    }

    //
    // Method which publishes a link to shaarli
    // Assume being logged in
    //
    public void postLink(String sharedUrl, String sharedTitle, String sharedDescription, String sharedTags, boolean privateShare)
            throws IOException {
        String encodedShareUrl = URLEncoder.encode(sharedUrl, "UTF-8");
        retrievePostLinkToken(encodedShareUrl);

        if (!isUrl(sharedUrl)) { // In case the url isn't really one, just post the one chosen by the server.
            sharedUrl = this.m_sharedUrl;
        }

        final String postUrl = this.m_shaarliUrl + "?post=" + encodedShareUrl;
        Connection postPageConn = Jsoup.connect(postUrl)
                .method(Connection.Method.POST)
                .timeout(this.m_timeout)
                .cookies(this.m_cookies)
                .timeout(10000)
                .data("save_edit", "Save")
                .data("token", this.m_token)
                .data("lf_tags", sharedTags)
                .data("lf_linkdate", this.m_datePostLink)
                .data("lf_url", sharedUrl)
                .data("lf_title", sharedTitle)
                .data("lf_description", sharedDescription);
        if (privateShare) postPageConn.data("lf_private", "on");
        postPageConn.execute(); // Then we post
    }

    //
    // Method which retrieve tags from the WS (old shaarli)
    // Assume being logged in
    //
    public String[] retrieveTagsFromWs() {
        final String requestUrl = this.m_shaarliUrl + "?ws=tags&term=+";
        String[] predictionsArr = {};
        try {
            String json = Jsoup.connect(requestUrl)
                    .timeout(this.m_timeout)
                    .cookies(this.m_cookies)
                    .ignoreContentType(true)
                    .execute()
                    .body();

            JSONArray ja = new JSONArray(json);
            predictionsArr = new String[ja.length()];
            for (int i = 0; i < ja.length(); i++) {
                // add each entry to our array
                predictionsArr[i] = ja.getString(i);
//                    Log.d("Shaarlier, tag :", ja.getString(i));
            }

        } catch (IOException | JSONException e) {
            return predictionsArr;
        }
        return predictionsArr;
    }

    //
    // Method which retrieve tags from awesomplete (new shaarli)
    // Assume being logged in
    //
    public String[] retrieveTagsFromAwesomplete() {
        final String requestUrl = this.m_shaarliUrl + "?post=";
        String[] tags = {};
        try {
            String tagsString = Jsoup.connect(requestUrl)
                    .timeout(this.m_timeout)
                    .cookies(this.m_cookies)
                    .execute()
                    .parse()
                    .body()
                    .select("input[name=lf_tags]")
                    .first()
                    .attr("data-list");
            tags = tagsString.split(", ");

        } catch (IOException | NullPointerException e) {
            return tags;
        }
        return tags;
    }
}