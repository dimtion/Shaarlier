package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Created by dimtion on 16/05/2015.
 * A class to help debugging, should not be in production
 */
class DebugHelper {

    public static void sendMailDev(Activity context, String subject, String content) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", context.getString(R.string.developer_mail), null));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        context.startActivity(Intent.createChooser(intent, "Debug report..."));
    }

    public static String generateReport(Exception e, Activity activity, String extra) {
        String[] errorMessage = {e.getMessage(), e.toString()};

        return generateReport(errorMessage, activity, extra);
    }

    public static String generateReport(String[] errorMessage, Activity activity, String extra){
        String message = "Feel free to add a little message : \n\n";

        message += "-----BEGIN REPORT-----\n";
        message += "Report type: DEBUG \n";
        message += "Android version: " + " " + Build.VERSION.RELEASE + "\n";
        try {
            message += "App version: " + activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName + "\n";
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }
        message += "Activity: " + activity.toString();

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        message += df.format(new Date()) + "\n\n";

        for (String m : errorMessage) {
            message += m + "\n\n";
        }

        message += "-----EXTRA-----\n" + extra + "\n";

        message += "-----END REPORT-----\n\n";
        message += "Thanks for the report, I'll try to answer as soon as possible !\n";

        return message;
    }
}