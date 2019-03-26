package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class UserPreferences {
    private boolean privateShare;
    private boolean openDialog;
    private boolean autoTitle;
    private boolean autoDescription;
    private boolean tweet;

    static UserPreferences load(Activity a) {
        UserPreferences p = new UserPreferences();
        SharedPreferences pref = a.getSharedPreferences(a.getString(R.string.params), MODE_PRIVATE);
        p.privateShare = pref.getBoolean(a.getString(R.string.p_default_private), true);
        p.openDialog = pref.getBoolean(a.getString(R.string.p_show_share_dialog), true);
        p.autoTitle = pref.getBoolean(a.getString(R.string.p_auto_title), true);
        p.autoDescription = pref.getBoolean(a.getString(R.string.p_auto_description), false);
        p.tweet = pref.getBoolean(a.getString(R.string.p_shaarli2twitter), false);

        return p;
    }

    boolean isPrivateShare() {
        return privateShare;
    }

    void setPrivateShare(boolean m_privateShare) {
        this.privateShare = m_privateShare;
    }

    boolean isOpenDialog() {
        return openDialog;
    }

    void setOpenDialog(boolean m_prefOpenDialog) {
        this.openDialog = m_prefOpenDialog;
    }

    boolean isAutoTitle() {
        return autoTitle;
    }

    void setAutoTitle(boolean m_autoTitle) {
        this.autoTitle = m_autoTitle;
    }

    boolean isAutoDescription() {
        return autoDescription;
    }

    void setAutoDescription(boolean m_autoDescription) {
        this.autoDescription = m_autoDescription;
    }

    boolean isTweet() {
        return tweet;
    }

    void setTweet(boolean tweet) {
        this.tweet = tweet;
    }
}

