package com.dimtion.shaarlier;

/**
 * Created by dimtion on 12/05/2015.
 * A tag from the SQLite db
 */
public class Tag {
    private long id;
    private ShaarliAccount masterAccount;
    private String value;
    private long masterAccountId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ShaarliAccount getMasterAccount() {
        return masterAccount;
    }

    public void setMasterAccount(ShaarliAccount masterAccount) {
        this.masterAccount = masterAccount;
        this.masterAccountId = masterAccount.getId();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getMasterAccountId() {
        return masterAccountId;
    }

    public void setMasterAccountId(long masterAccountId) {
        this.masterAccountId = masterAccountId;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
