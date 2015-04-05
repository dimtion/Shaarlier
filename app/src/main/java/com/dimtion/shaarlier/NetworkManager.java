package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.URLUtil;

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

    NetworkManager(String shaarliUrl, String username, String password) {
        this.m_shaarliUrl = shaarliUrl;
        this.m_username = username;
        this.m_password = password;
    }

    //
    // Check if a string is an url
    //
    public static boolean isUrl(String url) {
        return URLUtil.isValidUrl(url);
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
    // Method to load the title of a web page
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
    // Check the website is a compatible shaarli, by downloading a token
    //
    public boolean retrieveLoginToken() throws IOException {
        final String loginFormUrl = this.m_shaarliUrl + "?do=login";
        try {
            Connection.Response loginFormPage = Jsoup.connect(loginFormUrl)
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
    //
    public boolean login() throws IOException {
        final String loginUrl = this.m_shaarliUrl;
        try {
            Connection.Response loginPage = Jsoup.connect(loginUrl)
                    .method(Connection.Method.POST)
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
    //
    public void retrievePostLinkToken(String encodedSharedLink) throws IOException {
        final String postFormUrl = this.m_shaarliUrl + "?post=" + encodedSharedLink;
        Connection.Response postFormPage = Jsoup.connect(postFormUrl)
                .followRedirects(true)
                .cookies(this.m_cookies)
                .timeout(this.m_timeout)
                .execute();
        final Element postFormBody = postFormPage.parse().body();

        // Update our situation :
        this.m_token = postFormBody.select("input[name=token]").first().attr("value");
        this.m_datePostLink = postFormBody.select("input[name=lf_linkdate]").first().attr("value"); // Date choosen by the server
    }

    //
    // Method which publishes a link to shaarli
    //
    public void postLink(String sharedUrl, String sharedTitle, String sharedDescription, String sharedTags, boolean privateShare)
            throws IOException {
        String encodedShareUrl = URLEncoder.encode(sharedUrl, "UTF-8");
        retrievePostLinkToken(encodedShareUrl);

        final String postUrl = this.m_shaarliUrl + "?post=" + encodedShareUrl;
        Connection postPageConn = Jsoup.connect(postUrl)
                .method(Connection.Method.POST)
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
}