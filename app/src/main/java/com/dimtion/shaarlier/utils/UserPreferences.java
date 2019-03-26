package com.dimtion.shaarlier.utils;

import android.app.Activity;
import android.content.SharedPreferences;

import com.dimtion.shaarlier.R;

import static android.content.Context.MODE_PRIVATE;

public class UserPreferences {
    private boolean privateShare;
    private boolean openDialog;
    private boolean autoTitle;
    private boolean autoDescription;
    private boolean tweet;

    public static UserPreferences load(Activity a) {
        UserPreferences p = new UserPreferences();
        SharedPreferences pref = a.getSharedPreferences(a.getString(R.string.params), MODE_PRIVATE);
        p.privateShare = pref.getBoolean(a.getString(R.string.p_default_private), true);
        p.openDialog = pref.getBoolean(a.getString(R.string.p_show_share_dialog), true);
        p.autoTitle = pref.getBoolean(a.getString(R.string.p_auto_title), true);
        p.autoDescription = pref.getBoolean(a.getString(R.string.p_auto_description), false);
        p.tweet = pref.getBoolean(a.getString(R.string.p_shaarli2twitter), false);

        return p;
    }

    public boolean isPrivateShare() {
        return privateShare;
    }

    public void setPrivateShare(boolean m_privateShare) {
        this.privateShare = m_privateShare;
    }

    public boolean isOpenDialog() {
        return openDialog;
    }

    public void setOpenDialog(boolean m_prefOpenDialog) {
        this.openDialog = m_prefOpenDialog;
    }

    public boolean isAutoTitle() {
        return autoTitle;
    }

    public void setAutoTitle(boolean m_autoTitle) {
        this.autoTitle = m_autoTitle;
    }

    public boolean isAutoDescription() {
        return autoDescription;
    }

    public void setAutoDescription(boolean m_autoDescription) {
        this.autoDescription = m_autoDescription;
    }

    public boolean isTweet() {
        return tweet;
    }

    public void setTweet(boolean tweet) {
        this.tweet = tweet;
    }
}

