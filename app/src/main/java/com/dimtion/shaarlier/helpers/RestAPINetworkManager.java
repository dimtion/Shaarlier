package com.dimtion.shaarlier.helpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private static final String LINK_URL = "api/v1/links";

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
            JSONObject resp = new JSONObject(body);

            // For example check that the settings var exists
            if (resp.getJSONObject("settings") == null) {
                return false;
            }
        } catch (HttpStatusException e) {
            Log.w("RestAPINetworkManager", e.toString());
            if (e.getStatusCode() == 404) {
                return false;  // API V1 not supported
            } else {
                return e.getStatusCode() == 401;  // API V1 supported
            }
        } catch (JSONException e) {
            Log.e("RestAPINetworkManager", e.toString());
            return false;
        }
        // assume a 2XX or 3XX means API V1 supported
        return true;
    }

    @Override
    public boolean login() throws IOException {
        // TODO: we could set some account parameters from here like default_private_links
        String url = new URL(this.mAccount.getUrlShaarli() + INFO_URL).toExternalForm();
        try {
            Log.d("Login", this.mAccount.getRestAPIKey());
            String body = this.newConnection(url, Connection.Method.GET)
                    .execute()
                    .body();
            Log.i("Login", body);
        } catch (HttpStatusException e) {
            Log.w("Login", e);
            Log.w("Login", e.getMessage());
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
        String url = new URL(this.mAccount.getUrlShaarli() + LINK_URL).toExternalForm();
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
    public void pushLink(Link link) throws IOException {
        String url = new URL(link.getAccount().getUrlShaarli() + LINK_URL).toExternalForm();

        // Create the request body:
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("url", link.getUrl());
            requestBody.put("title", link.getTitle());
            requestBody.put("description", link.getDescription());
            requestBody.put("tags", link.getTags().split(","));
            requestBody.put("private", link.isPrivate());
            if (link.isTweet()) { // TODO tweet
                Log.e("RequestAPI:post", "Tweet feature not implemented");
            }
            if (link.isToot()) { // TODO toot
                Log.e("RequestAPI:post", "Toot feature not implemented");
            }
        } catch (JSONException e) {
            Log.e("RequestAPI:post", e.toString());
        }

        // 1. Try POST only if link id is null
        if (link.getId() == null) {
            Connection.Response resp = this.newConnection(url, Connection.Method.POST)
                    .requestBody(requestBody.toString())
                    .ignoreHttpErrors(true)
                    .execute();
            if (200 <= resp.statusCode() && resp.statusCode() <= 201) { // HTTP Ok after redirect
                return; // We are done here
            } else if (resp.statusCode() != 409) { // 409 HTTP Conflict
                // every other error we bubble up
                throw new HttpStatusException("Error requesting: " + url + " : " + resp.statusCode(), resp.statusCode(), url);
            }
            // On conflict, we update our id
            try {
                link.setId(new JSONObject(resp.body()).getInt("id"));
            } catch (JSONException e) {
                throw new IOException("Invalid id sent by Shaarli: " + e.toString());
            }
        }

        // 2. If POST failed or link had id try PUT:
        this.newConnection(url + "/" + link.getId(), Connection.Method.PUT)
                .requestBody(requestBody.toString())
                .ignoreHttpErrors(false)
                .execute();
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
                .header("Content-Type", "application/json")
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

    @Override
    public List<Link> getLinks(@Nullable Integer offset, @Nullable Integer limit) throws IOException, JSONException {
        String url = new URL(this.mAccount.getUrlShaarli() + LINK_URL).toExternalForm();
        String body = this.newConnection(url, Connection.Method.GET)
                .execute()
                .body();
        List<Link> links = new ArrayList<Link>();
        JSONArray resp = new JSONArray(body);
        for (int i = 0; i < resp.length(); i++) {
            JSONObject jsonLink = resp.getJSONObject(i);
            JSONArray jsonTags = jsonLink.getJSONArray("tags");
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < jsonTags.length(); j++) {
                tags.add(jsonTags.getString(j));
            }
            Link link = new Link(
                    jsonLink.getString("url"),
                    jsonLink.getString("title"),
                    jsonLink.getString("description"),
                    StringUtil.join(tags, ", "),
                    jsonLink.getBoolean("private"),
                    null, // TODO
                    false,
                    false,
                    jsonLink.getString("created"),
                    null
            );
            link.setId(jsonLink.getInt("id"));
            links.add(link);
        }
        return links;
    }

    /**
     * Inspired by https://gitlab.com/snippets/1665808
     * License: MIT
     * Copyright 2017 braincoke
     *
     * @return JWT encoded in base 64
     */
    String getJwt() {
        // iat in the payload
        Date date = new Date();
        // During debugging I found that given that some servers and phones are not absolutly in sync
        // It happens that the token would looked like being generated in the future
        // To compensate that we remove 5 from the actual date.
        date.setTime(date.getTime() - 5000);
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
