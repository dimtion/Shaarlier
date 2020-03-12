package com.dimtion.shaarlier.helpers;

import android.support.annotation.NonNull;
import android.util.Log;

import com.dimtion.shaarlier.utils.Link;
import com.dimtion.shaarlier.utils.ShaarliAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class RestAPINetworkManager implements NetworkManager {
    private static final String TAGS_URL = "api/v1/tags";
    private static final String INFO_URL = "api/v1/info";
    private static final String LINK_SEARCH = "api/v1/links";

    private ShaarliAccount mAccount;

    RestAPINetworkManager(@NonNull ShaarliAccount account) {
        this.mAccount = account;
    }

    @Override
    public boolean isCompatibleShaarli() throws IOException {
        String url = new URL(this.mAccount.getUrlShaarli() + INFO_URL).toExternalForm();
        try {
            String body = this.newConnection(url, Connection.Method.GET)
                    .execute()
                    .body();
            Log.i("RestAPINetworkManager", body);
        } catch (HttpStatusException e) {
            Log.w("RestAPINetworkManager", e.toString());
            if (e.getStatusCode() == 404) {
                return false;  // API V1 not supported
            } else return e.getStatusCode() == 401;  // API V1 supported
        }
        // assume a 2XX or 3XX means API V1 supported
        return true;
    }

    @Override
    public boolean login() throws IOException {
        // TODO: we could set some account parameters from here like default_private_links
        String url = new URL(this.mAccount.getUrlShaarli() + INFO_URL).toExternalForm();
        try {
            String body = this.newConnection(url, Connection.Method.GET)
                    .execute()
                    .body();
            Log.i("RestAPINetworkManager", body);
        } catch (HttpStatusException e) {
            return false;
        }
        return true;
    }

    @Override
    public Link prefetchLinkData(Link link) throws IOException {
        // TODO: There might be some bugs here, e.g:
        // - If the scheme used is not the same that on the saved link
        // - If there are tracking tags that don't match
        // We might want to open an Issue on Shaarli to get feedback
        String url = new URL(this.mAccount.getUrlShaarli() + LINK_SEARCH).toExternalForm();
        String body = this.newConnection(url, Connection.Method.GET)
                .data("offset", "0")
                .data("limit", "1")
                .data("searchterm", link.getUrl())
                .execute()
                .body();
        Log.d("RestAPI:prefetch", body);

        Link updatedLink = new Link(link);
        try {
            JSONArray resp = new JSONArray(body);
            if (resp.length() < 1) {
                Log.i("RestAPI:prefetch", "New link");
            } else {
                Log.i("RestAPI:prefetch", "Found 1 link result (not new link)");
                JSONObject returnedLink = resp.getJSONObject(0);
                updatedLink.setUrl(returnedLink.getString("url"));
                updatedLink.setTitle(returnedLink.getString("title"));
                updatedLink.setDescription(returnedLink.getString("description"));
                updatedLink.setPrivate(returnedLink.getBoolean("private"));
                JSONArray jsonTags = returnedLink.getJSONArray("tags");
                ArrayList<String> tags = new ArrayList<>();
                for (int i = 0; i < jsonTags.length(); i++) {
                    tags.add(jsonTags.getString(i));
                }
                updatedLink.setTags(StringUtil.join(tags, ", "));
            }
        } catch (JSONException e) {
            Log.e("RestAPI:prefetch", e.toString());
        }
        return updatedLink;
    }

    @Override
    public void postLink(String sharedUrl, String sharedTitle, String sharedDescription, String sharedTags, boolean privateShare, boolean tweet, boolean toot) throws IOException {
        // TODO
    }

    /**
     * Helper method to a new connection to the shaarli instance
     *
     * @param url
     * @param method
     * @return
     */
    private Connection newConnection(String url, Connection.Method method) {
        return Jsoup.connect(url)
                .header("Authorization", "Bearer " + this.getJwt())
                .ignoreContentType(true) // application/json
                .validateTLSCertificates(this.mAccount.isValidateCert())
                .timeout(NetworkUtils.TIME_OUT)
                .followRedirects(true)
                .method(method);
    }

    @Override
    public List<String> retrieveTags() throws Exception {
        String url = new URL(this.mAccount.getUrlShaarli() + TAGS_URL).toExternalForm();
        String body = this.newConnection(url, Connection.Method.GET)
                .execute()
                .body();
        List<String> tags = new ArrayList<String>();
        JSONArray resp = new JSONArray(body);
        for (int i = 0; i < resp.length(); i++) {
            tags.add(resp.getJSONObject(i).getString("name"));
        }
        return tags;
    }

    /**
     * Inspired by https://gitlab.com/snippets/1665808
     * License: MIT
     * Copyright 2017 braincoke
     *
     * @return JWT encoded in base 64
     */
    String getJwt() {
        // The date the token is issued at, will be mapped to the key "iat" in the payload
        Date date = new Date();
        // The key used to sign the token, you can find it by logging to your Shaarli instance
        // and then going to "Tools"
        byte[] signingKey = this.mAccount.getRestAPIKey().getBytes();
        // We create the token with the Jwts library
        return Jwts.builder()
                .setIssuedAt(date)
                .setHeaderParam("typ", "JWT")
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }
}
