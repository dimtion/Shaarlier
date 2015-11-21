package com.dimtion.shaarlier;

import java.io.Serializable;

/**
 * Created by dimtion on 11/05/2015.
 * A Shaarli Account
 */

public class ShaarliAccount implements Serializable {
    private long id;
    private String urlShaarli;
    private String username;
    private String password;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String shortName;
    private byte[] initialVector;
    private boolean validateCert;

    @Override
    public String toString() {
        if (this.shortName.equals(""))
            return username;
        return shortName;
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

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public void setBasicAuthUsername(String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public void setBasicAuthPassword(String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public byte[] getInitialVector() {
        return initialVector;
    }

    public void setInitialVector(byte[] initialVector) {
        this.initialVector = initialVector;
    }

    public boolean isValidateCert() {
        return validateCert;
    }

    public void setValidateCert(boolean validateCert) {
        this.validateCert = validateCert;
    }
}
