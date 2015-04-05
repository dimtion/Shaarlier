package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.URLUtil;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Map;

/**
 * Created by dimtion on 05/04/2015.
 * This class handle all the communications with shaarli and other web services
 */
class NetworkManager {
    private String m_shaarliUrl;
    private String m_username;
    private String m_password;

    private Activity m_parentActivity;

    private Map<String, String> m_cookies;
    private String m_token;

    NetworkManager(Activity parentActivity, String shaarliUrl, String username, String password) {
        this.m_shaarliUrl = shaarliUrl;
        this.m_username = username;
        this.m_password = password;
        this.m_parentActivity = parentActivity;
    }

    //
    // Check if a string is an url
    //
    public static boolean isUrl(String url) {
        return URLUtil.isValidUrl(url);
    }

    //
    // Method to test the network connection
    //
    public boolean testNetwork() {
        ConnectivityManager connMgr = (ConnectivityManager) this.m_parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
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
}