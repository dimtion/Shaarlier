package com.dimtion.shaarlier;

class Link {
    private String url;
    private String title;
    private String description;
    private String tags;
    private boolean isPrivate;
    private ShaarliAccount account;
    private boolean tweet;

    Link(String url, String title, String description, String tags, boolean isPrivate, ShaarliAccount account, boolean tweet) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.isPrivate = isPrivate;
        this.account = account;
        this.tweet = tweet;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getTags() {
        return tags;
    }

    void setTags(String tags) {
        this.tags = tags;
    }

    boolean isPrivate() {
        return isPrivate;
    }

    void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    ShaarliAccount getAccount() {
        return account;
    }

    void setAccount(ShaarliAccount account) {
        this.account = account;
    }

    boolean isTweet() {
        return tweet;
    }

    void setTweet(boolean tweet) {
        this.tweet = tweet;
    }
}
