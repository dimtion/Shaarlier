package com.dimtion.shaarlier.helpers;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.URLUtil;

import com.dimtion.shaarlier.utils.ShaarliAccount;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class NetworkUtils {
    protected static final int TIME_OUT = 60_000; // Better for mobile connections

    private static final int LOAD_TITLE_MAX_BODY_SIZE = 50240;
    private static final String[] DESCRIPTION_SELECTORS = {
            "meta[property=og:description]",
            "meta[name=description]",
            "meta[name=twitter:description]",
            "meta[name=mastodon:description]",
    };

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
     *
     * @return true if the device is connected to the network
     */
    public static boolean testNetwork(@NonNull Activity parentActivity) {
        ConnectivityManager connMgr = (ConnectivityManager) parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Static method to load the title of a web page
     *
     * @param url the url of the web page
     * @return "" if there is an error, the page title in other cases
     */
    public static String[] loadTitleAndDescription(@NonNull String url) {
        String title = "";
        String description = "";
        Document pageResp;
        try {
            pageResp = Jsoup.connect(url)
                    .maxBodySize(NetworkUtils.LOAD_TITLE_MAX_BODY_SIZE) // Hopefully we won't need more data
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
        for (String selector : NetworkUtils.DESCRIPTION_SELECTORS) {
            try {
                description = pageResp.head().select(selector).first().attr("content");
            } catch (Exception e) {
                Log.i("NetworkManager", e.toString());
            }
            if (!"".equals(description)) {
                break;
            }
        }
        return new String[]{title, description};
    }

    /**
     * Select the correct network manager based on the passed account
     *
     * @param account
     * @return
     */
    public static NetworkManager getNetworkManager(ShaarliAccount account) {
        // TODO: for debugging purposes
        account.setRestAPIKey("dummy test token");
        if (account.getRestAPIKey() != null && account.getRestAPIKey().length() > 0) {
            return new RestAPINetworkManager(account);
        } else {
            return new PasswordNetworkManager(account);
        }
    }
}
