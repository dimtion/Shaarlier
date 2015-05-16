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
public class DebugHelper {
    public static final String DEVELOPER_MAIL = "zizou.xena@gmail.com";

    public static void sendMailDev(Activity context, String subject, String content) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", DEVELOPER_MAIL, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        context.startActivity(Intent.createChooser(intent, "Debug report..."));
    }

    public static String generateReport(Exception e, Activity activity, String extra) {
        String message = "-----BEGIN REPORT-----\n";
        message += "Report type: DEBUG \n";
        message += "Android version: " + " " + Build.VERSION.RELEASE + "\n";
        try {
            message += "App version: " + activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName + "\n";
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        message += df.format(new Date()) + "\n\n";

        message += e.getMessage() + "\n\n";
        message += e.toString() + "\n\n";

        message += "-----EXTRA-----\n" + extra + "\n";

        message += "-----END REPORT-----\n";
        return message;
    }
}