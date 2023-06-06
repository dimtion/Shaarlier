package com.dimtion.shaarlier.helper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ShareCompat;

import com.dimtion.shaarlier.exceptions.UnsupportedIntent;
import com.dimtion.shaarlier.models.Link;
import com.dimtion.shaarlier.models.ShaarliAccount;
import com.dimtion.shaarlier.network.NetworkUtils;
import com.dimtion.shaarlier.utils.UserPreferences;

import java.util.Objects;

public class ShareIntentParser {

    private static final String LOGGER_NAME = "ShareIntentParser";

    private static final String EXTRA_TAGS = "tags";
    private static final String EXTRA_DESCRIPTION = "description";

    private static final String TEXT_MIME_TYPE = "text/plain";

    private final UserPreferences userPreferences;
    private final ShaarliAccount shaarliAccount;

    public ShareIntentParser(final ShaarliAccount shaarliAccount, final UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
        this.shaarliAccount = shaarliAccount;
    }

    /**
     * Create a populated {@link Link} from an {@link Intent}.
     *
     * @param activity source activity
     * @param intent   intent to parse
     * @return a populated Link
     * @throws UnsupportedIntent if throwing an unsupported intent.
     */
    public Link parse(final Activity activity, final Intent intent) throws UnsupportedIntent {
        if (!Intent.ACTION_SEND.equals(intent.getAction()) || !TEXT_MIME_TYPE.equals(intent.getType())) {
            Log.e(LOGGER_NAME, "Unsupported action " + intent.getAction() + " or mime type " + intent.getType());
            throw new UnsupportedIntent();
        }

        final ShareCompat.IntentReader reader = ShareCompat.IntentReader.from(activity);

        final String url = extractUrl(reader.getText().toString());
        final String title = userPreferences.isAutoTitle() ? extractTitle(reader.getSubject()) : "";
        final String defaultTags = intent.getStringExtra(EXTRA_TAGS) != null ?
                intent.getStringExtra(EXTRA_TAGS) : "";
        final String description;
        if (userPreferences.isAutoDescription() && Objects.nonNull(intent.getStringExtra(EXTRA_DESCRIPTION))) {
            description = intent.getStringExtra(EXTRA_DESCRIPTION);
        } else {
            description = "";
        }

        return new Link(
                url,
                title,
                description,
                defaultTags,
                userPreferences.isPrivateShare(),
                shaarliAccount,
                userPreferences.isTweet(),
                userPreferences.isToot(),
                null,
                null
        );
    }

    /**
     * Extract an url located in a text
     *
     * @param text: a text containing an url
     * @return url present in the input text
     */
    private String extractUrl(final String text) {
        String finalUrl;

        // Trim the url because for annoying apps that send to much data:
        finalUrl = text.trim();

        String[] possible_urls = finalUrl.split(" ");

        for (String url : possible_urls) {
            if (NetworkUtils.isUrl(url)) {
                finalUrl = url;
                break;
            }
        }

        finalUrl = finalUrl.substring(finalUrl.lastIndexOf(" ") + 1);
        finalUrl = finalUrl.substring(finalUrl.lastIndexOf("\n") + 1);

        // If the url is incomplete:
        if (NetworkUtils.isUrl("http://" + finalUrl) && !NetworkUtils.isUrl(finalUrl)) {
            finalUrl = "http://" + finalUrl;
        }
        // Delete trackers:
        if (finalUrl.contains("&utm_source=")) {
            finalUrl = finalUrl.substring(0, finalUrl.indexOf("&utm_source="));
        }
        if (finalUrl.contains("?utm_source=")) {
            finalUrl = finalUrl.substring(0, finalUrl.indexOf("?utm_source="));
        }
        if (finalUrl.contains("#xtor=RSS-")) {
            finalUrl = finalUrl.substring(0, finalUrl.indexOf("#xtor=RSS-"));
        }

        return finalUrl;
    }

    /**
     * Extract the title from a subject line
     *
     * @param subject: intent subject
     * @return Title
     */
    private String extractTitle(final String subject) {
        if (subject != null && !NetworkUtils.isUrl(subject)) {
            return subject;
        }

        return "";
    }
}
