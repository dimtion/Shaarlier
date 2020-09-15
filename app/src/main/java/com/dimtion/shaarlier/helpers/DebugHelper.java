package com.dimtion.shaarlier.helpers;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.dimtion.shaarlier.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Created by dimtion on 16/05/2015.
 * A class to help debugging and error reporting
 */
public class DebugHelper {

    public static void sendMailDev(Activity context, String subject, String content) {
        Log.d("sendMailDev", content);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getString(R.string.developer_mail)});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        context.startActivity(intent);
    }

    public static String generateReport(Exception e, Activity activity, String extra) {
        String[] errorMessage = {e.getMessage(), e.toString()};

        return generateReport(errorMessage, activity, extra);
    }

    public static String generateReport(String[] errorMessage, Activity activity, String extra) {
        String message = "Feel free to add a message explaining the circumstances of the error below, I'll do my best to help you:\n\n\n\n";

        message += "-------------\n";
        message += "Also make sure that your Shaarli instance is correctly setup. Common causes for issues:\n";
        message += " - RestAPI not enabled in Shaarli settings\n";
        message += " - mod_rewrite not enabled on Apache\n";
        message += " - Using password authentication with a custom theme\n";
        message += " - Using a hosting provider that injects Javascript in webpage.\n\n";

        message += "Thanks for the report, I'll try to answer as soon as possible!\n\n";

        message += "Bugtracker: https://github.com/dimtion/Shaarlier/issues\n\n";

        message += "-------------\n";

        message += "Below this line is an automated report, if you don't feel comfortable sharing ";
        message += "some of these fields please remove them. This data is  exclusively used for debugging.\n\n";

        message += "-----BEGIN REPORT-----\n";
        message += "Report type: ERROR \n";
        message += "Android version: " + " " + Build.VERSION.RELEASE + "\n";
        try {
            message += "App version: " + activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName + "\n";
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }
        message += "Activity: " + activity.toString() + "\n";

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        message += "Date: " + df.format(new Date()) + "\n\n";

        for (String m : errorMessage) {
            message += m + "\n\n";
        }

        message += "-----EXTRA-----\n" + extra + "\n";

        message += "-----END REPORT-----\n\n";
        return message;
    }
}