package com.dimtion.shaarlier.network;

import android.support.annotation.NonNull;
import android.util.Log;

import com.dimtion.shaarlier.models.Link;
import com.dimtion.shaarlier.models.ShaarliAccount;

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

    private final static String LOGGER_NAME = NetworkUtils.class.getSimpleName();

    private final ShaarliAccount mAccount;

    RestAPINetworkManager(@NonNull ShaarliAccount account) {
        this.mAccount = account;
    }

    @Override
    public boolean isCompatibleShaarli() throws IOException {
        String url = new URL(this.mAccount.getUrlShaarli() + INFO_URL).toExternalForm();
        Log.i(LOGGER_NAME, "Check Shaarli compatibility");
        try {
            final String body = this.newConnection(url, Connection.Method.GET)
                    .execute()
                    .body();
            Log.d(LOGGER_NAME, "JWT used: " + body);
            final JSONObject resp = new JSONObject(body);

            // For example check that the settings var exists
            if (resp.getJSONObject("settings") == null) {
                Log.i(LOGGER_NAME, "Settings var does not exists");
                return false;
            }
        } catch (final HttpStatusException e) {
            Log.w(LOGGER_NAME, "Exception while calling Shaarli: " + e.getMessage(), e);
            if (e.getStatusCode() == 404) {
                Log.i(LOGGER_NAME, "API V1 not supported");
                return false;  // API V1 not supported
            } else {
                return e.getStatusCode() == 401;  // API V1 supported
            }
        } catch (final JSONException e) {
            Log.e(LOGGER_NAME, "JSONException while calling shaarli: " + e.getMessage(), e);
            return false;
        } catch (final IllegalArgumentException e) {
            // This exception arises in a bug in JWT module. I added that to help with the debugging
            Log.e(LOGGER_NAME, "Error generating JWT: " + e.getMessage(), e);
            throw new IOException("isCompatibleShaarli: " + e, e);
        }
        // assume a 2XX or 3XX means API V1 supported
        Log.i(LOGGER_NAME, "API V1 supported");
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
            requestBody.put("tags", new JSONArray(link.getTagList()));
            requestBody.put("private", link.isPrivate());
            if (link.isTweet()) { // TODO tweet
                Log.e("RequestAPI:post", "Tweet feature not implemented");
            }
            if (link.isToot()) { // TODO toot
                Log.e("RequestAPI:post", "Toot feature not implemented");
            }
            Log.d("PushLink", requestBody.toString(2));
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
                throw new IOException("Invalid id sent by Shaarli: " + e);
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
     * @param url    to connect to
     * @param method HTTP method
     * @return a new opened connection
     */
    private Connection newConnection(String url, Connection.Method method) {
        Log.i(LOGGER_NAME, "Creating new connection " + url + " : " + method);
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

    /**
     * Inspired by <a href="https://gitlab.com/snippets/1665808">gitlab</a>
     * License: MIT
     * Copyright 2017 braincoke
     *
     * @return JWT encoded in base 64
     */
    String getJwt() {
        // TODO: we are obligated to stay with jjwt 0.9.1 because of Shaarli weak keys

        Log.i("JWT", "Generating JWT for account " + this.mAccount);
        // iat in the payload
        Date date = new Date();
        // During debugging I found that given that some servers and phones are not absolutely in sync
        // It happens that the token would looked like being generated in the future
        // To compensate that we remove 5 seconds from the actual date.
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
