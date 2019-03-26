package com.dimtion.shaarlier.utils;

public class Link {
    private String url;
    private String title;
    private String description;
    private String tags;
    private boolean isPrivate;
    private ShaarliAccount account;
    private boolean tweet;

    public Link(String url, String title, String description, String tags, boolean isPrivate, ShaarliAccount account, boolean tweet) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.isPrivate = isPrivate;
        this.account = account;
        this.tweet = tweet;
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

    public String getTags() {
        return tags;
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
}
