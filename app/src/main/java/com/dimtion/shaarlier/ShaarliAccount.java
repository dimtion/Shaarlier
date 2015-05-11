package com.dimtion.shaarlier;

/**
 * Created by dimtion on 11/05/2015.
 */
public class ShaarliAccount {
    private long id;
    private String urlShaarli;
    private String username;
    private String password;
    private String shortName;

    @Override
    public String toString() {
        return username;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrlShaarli() {
        return urlShaarli;
    }

    public void setUrlShaarli(String urlShaarli) {
        this.urlShaarli = urlShaarli;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
