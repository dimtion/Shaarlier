package com.dimtion.shaarlier.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Link implements Serializable {
    private Integer id;
    private String url;
    private String title;
    private String description;
    private String tags;
    private boolean isPrivate;
    private ShaarliAccount account;
    private boolean tweet;
    private boolean toot;

    private String datePostLink;
    private String token;

    public Link(String url, String title, String description, String tags, boolean isPrivate, ShaarliAccount account, boolean tweet, boolean toot, String datePostLink, String token) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.isPrivate = isPrivate;
        this.account = account;
        this.tweet = tweet;
        this.toot = toot;
        this.datePostLink = datePostLink;
        this.token = token;
    }

    public Link(Link other) {
        this.id = other.id;
        this.url = other.url;
        this.title = other.title;
        this.description = other.description;
        this.tags = other.tags;
        this.isPrivate = other.isPrivate;
        this.account = other.account;
        this.tweet = other.tweet;
        this.toot = other.toot;
        this.datePostLink = other.datePostLink;
        this.token = other.token;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Deprecated
    public String getTags() {
        return tags;
    }

    public List<String> getTagList() {
        return Arrays.asList(tags.split(", "));
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public ShaarliAccount getAccount() {
        return account;
    }

    public void setAccount(ShaarliAccount account) {
        this.account = account;
    }

    public boolean isTweet() {
        return tweet;
    }

    public void setTweet(boolean tweet) {
        this.tweet = tweet;
    }

    public boolean isToot() {
        return toot;
    }

    public void setToot(boolean toot) {
        this.toot = toot;
    }

    public String getDatePostLink() {
        return datePostLink;
    }

    public void setDatePostLink(String datePostLink) {
        this.datePostLink = datePostLink;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean seemsNotNew() {
        return (
                (description != null && !description.trim().equals(""))
                || (tags != null && !tags.trim().equals(""))
                        || id != null
        );
    }

}
