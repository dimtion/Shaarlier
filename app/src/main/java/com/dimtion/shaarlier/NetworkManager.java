package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.webkit.URLUtil;

import org.json.JSONArray;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by dimtion on 05/04/2015.
 * This class handle all the communications with Shaarli and other web services
 */
class NetworkManager {
    private static final int LOAD_TITLE_MAX_BODY_SIZE = 50240;
    private static final int DEFAULT_TIME_OUT = 10000;

    private static final String[] DESCRIPTION_SELECTORS = {
            "meta[property=og:description]",
            "meta[name=description]",
            "meta[name=twitter:description]",
    };

    private final String mShaarliUrl;
    private final String mUsername;
    private final String mPassword;
    private final boolean mValidateCert;
    private final String mBasicAuth;
    private Integer mTimeout = DEFAULT_TIME_OUT;

    private Map<String, String> mCookies;
    private String mToken;

    private String mDatePostLink;
    private String mSharedUrl;
    private Exception mLastError;

    public Exception getLastError() {
        return mLastError;
    }

    NetworkManager(ShaarliAccount account) {
        this.mShaarliUrl = account.getUrlShaarli();
        this.mUsername = account.getUsername();
        this.mPassword = account.getPassword();
        this.mValidateCert = account.isValidateCert();

        if (!"".equals(account.getBasicAuthUsername())  && !"".equals(account.getBasicAuthPassword())) {
            String login = account.getBasicAuthUsername() + ":" + account.getBasicAuthPassword();
            this.mBasicAuth = new String(Base64.encode(login.getBytes(), Base64.NO_WRAP));
        } else {
            this.mBasicAuth = "";
        }
    }

    /**
     * Helper method which create a new connection to Shaarli
     * @param url the url of the shaarli
     * @param isPost true if we create a POST request, false for a GET request
     * @return pre-made jsoupConnection
     */
    private Connection createShaarliConnection(String url, boolean isPost){
        Connection jsoupConnection = Jsoup.connect(url);

        Connection.Method connectionMethod = isPost ? Connection.Method.POST : Connection.Method.GET;
        if (!"".equals(this.mBasicAuth)) {
            jsoupConnection = jsoupConnection.header("Authorization", "Basic " + this.mBasicAuth);
        }
        if (this.mCookies != null){
            jsoupConnection = jsoupConnection.cookies(this.mCookies);
        }

        return jsoupConnection
                .validateTLSCertificates(this.mValidateCert)
                .timeout(this.mTimeout)
                .followRedirects(true)
                .method(connectionMethod);
    }

    /**
     * Check if a string is an url
     * TODO : unit test on this, I'm not quite sure it is perfect...
     */
    public static boolean isUrl(String url) {
        return URLUtil.isValidUrl(url) && !"http://".equals(url);
    }

    /**
     * Change something which is close to a url to something that is really one
     */
    public static String toUrl(String givenUrl) {
        String finalUrl = givenUrl;
        String protocol = "http://";  // Default value
        if ("".equals(givenUrl)) {
            return givenUrl;  // Edge case, maybe need some discussion
        }

        if (!finalUrl.endsWith("/")) {
            finalUrl += '/';
        }

        if (!(finalUrl.startsWith("http://") || finalUrl.startsWith("https://"))) {
            finalUrl = protocol + finalUrl;
        }

        return finalUrl;
    }

    /**
     * Method to test the network connection
     * @return true if the device is connected to the network
     */
    public static boolean testNetwork(Activity parentActivity) {
        ConnectivityManager connMgr = (ConnectivityManager) parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Static method to load the title of a web page
     * @param url the url of the web page
     * @return "" if there is an error, the page title in other cases
     */
    public static String[] loadTitleAndDescription(String url) {
        String title = "";
        String description = "";
        Document pageResp;
        try {
            pageResp = Jsoup.connect(url)
                    .maxBodySize(LOAD_TITLE_MAX_BODY_SIZE) // Hopefully we won't need more data
                    .followRedirects(true)
                    .execute()
                    .parse();
            title = pageResp.title();
        } catch (Exception e) {
            // Just abandon the task if there is a problem
            Log.e("NetworkManager", e.toString());
            return new String[]{title, description};
        }

        // Many ways to get the description
        for (String selector : DESCRIPTION_SELECTORS) {
            try {
                description = pageResp.head().select(selector).first().attr("content");
            }
            catch (Exception e){
                Log.i("NetworkManager", e.toString());
            }
            if (!"".equals(description)){
                break;
            }
        }
        return new String[]{title, description};
    }

    /**
     * Set the default timeout
     * @param timeout : timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }

    /**
     * Check the if website is a compatible shaarli, by downloading a token
     */
    public boolean retrieveLoginToken() throws IOException {
        final String loginFormUrl = this.mShaarliUrl + "?do=login";
        try {

            Connection.Response loginFormPage = this.createShaarliConnection(loginFormUrl, false).execute();
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
    public boolean login() throws IOException {
        final String loginUrl = this.mShaarliUrl;
        try {
            Connection.Response loginPage = this.createShaarliConnection(loginUrl, true)
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

        Connection.Response postFormPage = this.createShaarliConnection(postFormUrl, false)
                .execute();
        final Element postFormBody = postFormPage.parse().body();

        // Update our situation :
        this.mToken = postFormBody.select("input[name=token]").first().attr("value");
        this.mDatePostLink = postFormBody.select("input[name=lf_linkdate]").first().attr("value"); // Date choosen by the server
        this.mSharedUrl = postFormBody.select("input[name=lf_url]").first().attr("value");
    }

    /**
     * Method which publishes a link to shaarli
     * Assume being logged in
     */
    public void postLink(String sharedUrl, String sharedTitle, String sharedDescription, String sharedTags, boolean privateShare)
            throws IOException {
        String encodedShareUrl = URLEncoder.encode(sharedUrl, "UTF-8");
        retrievePostLinkToken(encodedShareUrl);

        if (isUrl(sharedUrl)) { // In case the url isn't really one, just post the one chosen by the server.
            this.mSharedUrl = sharedUrl;
        }

        final String postUrl = this.mShaarliUrl + "?post=" + encodedShareUrl;

        Connection postPageConn = this.createShaarliConnection(postUrl, true)
                .data("save_edit", "Save")
                .data("token", this.mToken)
                .data("lf_tags", sharedTags)
                .data("lf_linkdate", this.mDatePostLink)
                .data("lf_url", this.mSharedUrl)
                .data("lf_title", sharedTitle)
                .data("lf_description", sharedDescription);
        if (privateShare) postPageConn.data("lf_private", "on");
        postPageConn.execute(); // Then we post
    }

    /**
     * Method which retrieve tags from the WS (old shaarli)
     * Assume being logged in
     */
    public String[] retrieveTagsFromWs() {
        final String requestUrl = this.mShaarliUrl + "?ws=tags&term=+";
        String[] predictionsArr = {};
        try {
            String json = this.createShaarliConnection(requestUrl, true)
                    .ignoreContentType(true)
                    .execute()
                    .body();

            JSONArray ja = new JSONArray(json);
            predictionsArr = new String[ja.length()];
            for (int i = 0; i < ja.length(); i++) {
                // add each entry to our array
                predictionsArr[i] = ja.getString(i);
            }

        } catch (Exception e) {
            this.mLastError = e;
            return predictionsArr;
        }
        return predictionsArr;
    }

    /**
     * Method which retrieve tags from awesomplete (new shaarli)
     * Assume being logged in
     */
    public String[] retrieveTagsFromAwesomplete() {
        final String requestUrl = this.mShaarliUrl + "?post=";
        String[] tags = {};
        try {
            String tagsString = this.createShaarliConnection(requestUrl, false)
                    .execute()
                    .parse()
                    .body()
                    .select("input[name=lf_tags]")
                    .first()
                    .attr("data-list");
            tags = tagsString.split(", ");

        } catch (Exception e) {
            this.mLastError = e;
        }

        return tags;
    }
}