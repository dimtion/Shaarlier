package com.dimtion.shaarlier;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.io.IOException;

public class NetworkService extends IntentService {
    public static final String EXTRA_MESSENGER="com.dimtion.shaarlier.networkservice.EXTRA_MESSENGER";
    static final int NO_ERROR = 0;
    static final int NETWORK_ERROR = 1;
    static final int TOKEN_ERROR = 2;
    static final int LOGIN_ERROR = 3;

    private Exception mError;
    private ShaarliAccount mShaarliAccount;
    public NetworkService() {
        super("NetworkService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Load the messenger to communicate back to the activity
        Messenger messenger = (Messenger)intent.getExtras().get(EXTRA_MESSENGER);
        Message msg = Message.obtain();
        String action = intent.getStringExtra("action");


        if(action.equals("checkShaarli")){
            String urlShaarli = intent.getStringExtra("urlShaarli");
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");
            msg.arg1 = checkShaarli(urlShaarli, username, password);
            if(msg.arg1 == NETWORK_ERROR){
                msg.obj = mError;
            }
            // Send back messages to the calling activity
            try {
                assert messenger != null;
                messenger.send(msg);
            }
            catch (android.os.RemoteException | AssertionError e1) {
                Log.w(getClass().getName(), "Exception sending message", e1);

            }
        } else {
            if (action.equals("postLink")) {
                String sharedUrl = intent.getStringExtra("sharedUrl");
                String title = intent.getStringExtra("title");
                String description = intent.getStringExtra("description");
                String tags = intent.getStringExtra("tags");
                boolean isPrivate = intent.getBooleanExtra("privateShare", true);
                int accountId = intent.getIntExtra("chosenAccountId", -1);

                try {
                    AccountsSource acs = new AccountsSource(this);
                    this.mShaarliAccount = (accountId != -1 ? acs.getShaarliAccountById(accountId) : acs.getDefaultAccount());
                } catch (Exception e) {
                    e.printStackTrace();
                    sendNotificationShareError(sharedUrl, title, description, tags, isPrivate);
                    stopSelf();
                }
                // TODO : wait for title to be retrieved.
                postLink(sharedUrl, title, description, tags, isPrivate);
            }
        }


        stopSelf();
    }

    private int checkShaarli(String urlShaarli, String username, String password){
        NetworkManager manager = new NetworkManager(urlShaarli, username, password);
        try {
            if (!manager.retrieveLoginToken()) {
                return TOKEN_ERROR;
            }
            if (!manager.login()) {
                return LOGIN_ERROR;
            }
        } catch (IOException e) {
            mError = e;
            return NETWORK_ERROR;

        }
        return NO_ERROR;
    }

    private void postLink(String sharedUrl, String title, String description, String tags, boolean privateShare){
        boolean posted = true;  // Assume it is shared
        try {
            // Connect the user to the site :
            NetworkManager manager = new NetworkManager(
                    mShaarliAccount.getUrlShaarli(),
                    mShaarliAccount.getUsername(),
                    mShaarliAccount.getPassword());
            manager.setTimeout(60000); // Long for slow networks
            if(manager.retrieveLoginToken() && manager.login()) {
                manager.postLink(sharedUrl, title, description, tags, privateShare);
            } else {
                mError = new Exception("Could not connect to the shaarli. Possibles causes : unhandled shaarli, bad username or password");
                posted =  false;
            }
        } catch (IOException | NullPointerException e) {
            mError = e;
            Log.e("ERROR", e.getMessage());
            posted = false;
        }

        if (!posted) {
//            Toast.makeText(getApplicationContext(), R.string.add_error + " : " + mError.getMessage(), Toast.LENGTH_LONG).show();
            sendNotificationShareError(sharedUrl, title, description, tags, privateShare);
//            sendReport(mError, chosenAccount);
        } else {
            Log.i("SUCCESS", "Success while sharing link");
        }
    }

    private void sendNotificationShareError(String sharedUrl, String title, String description, String tags, boolean privateShare){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Failed to share " + title)
                        .setContentText("Press to try again")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, NetworkService.class);

//        resultIntent.setAction(Intent.ACTION_SEND);
//        resultIntent.setType("text/plain");
        resultIntent.putExtra("action", "postLink");
        resultIntent.putExtra("sharedUrl", sharedUrl);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("tags", tags);
        resultIntent.putExtra("privateShare", privateShare);
        resultIntent.putExtra("chosenAccountId", this.mShaarliAccount.getId());


        resultIntent.putExtra(Intent.EXTRA_TEXT, sharedUrl);
        resultIntent.putExtra(Intent.EXTRA_SUBJECT, title);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = PendingIntent.getService(this, 0, resultIntent, 0);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(sharedUrl.hashCode(), mBuilder.build());
    }
}