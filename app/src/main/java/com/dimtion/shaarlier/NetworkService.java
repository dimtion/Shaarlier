package com.dimtion.shaarlier;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class NetworkService extends IntentService {
    public static final String EXTRA_MESSENGER="com.dimtion.shaarlier.networkservice.EXTRA_MESSENGER";
    public static final int NO_ERROR = 0;
    public static final int NETWORK_ERROR = 1;
    public static final int TOKEN_ERROR = 2;
    public static final int LOGIN_ERROR = 3;

    public static final int RETRIEVE_TITLE_ID = 1;

    private String loadedTitle;

    private Context mContext;
    private Handler mToastHandler;
    private Exception mError;
    private ShaarliAccount mShaarliAccount;
    private String loadedDescription;

    public NetworkService() {
        super("NetworkService");
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mContext = this;
        mToastHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Load the messenger to communicate back to the activity
        Messenger messenger = (Messenger)intent.getExtras().get(EXTRA_MESSENGER);
        Message msg = Message.obtain();
        String action = intent.getStringExtra("action");


        switch (action) {
            case "checkShaarli":
                ShaarliAccount accountToTest = (ShaarliAccount) intent.getSerializableExtra("account");

                msg.arg1 = checkShaarli(accountToTest);
                if (msg.arg1 == NETWORK_ERROR) {
                    msg.obj = mError;
                }
                // Send back messages to the calling activity
                try {
                    assert messenger != null;
                    messenger.send(msg);
                } catch (android.os.RemoteException | AssertionError e1) {
                    Log.w(getClass().getName(), "Exception sending message", e1);
                }
                break;
            case "postLink":
                String sharedUrl = intent.getStringExtra("sharedUrl");
                String title = intent.getStringExtra("title");
                String description = intent.getStringExtra("description");
                String tags = intent.getStringExtra("tags");
                boolean isPrivate = intent.getBooleanExtra("privateShare", true);
                if ("".equals(title) && this.loadedTitle != null) {
                    title = this.loadedTitle;
                    this.loadedTitle = null;
                }
                if ("".equals(description) && this.loadedDescription != null) {
                    description = this.loadedDescription;
                    this.loadedDescription = null;
                }
                long accountId = intent.getLongExtra("chosenAccountId", -1);

                try {
                    AccountsSource acs = new AccountsSource(this);
                    this.mShaarliAccount = (accountId != -1 ? acs.getShaarliAccountById(accountId) : acs.getDefaultAccount());
                } catch (Exception e) {
                    e.printStackTrace();
                    sendNotificationShareError(sharedUrl, title, description, tags, isPrivate);
                }
                postLink(sharedUrl, title, description, tags, isPrivate);
                stopSelf();
                break;
            case "retrieveTitleAndDescription":
                this.loadedTitle = "";
                this.loadedDescription = "";

                String url = intent.getStringExtra("url");

                boolean autoTitle = intent.getBooleanExtra("autoTitle", true);
                boolean autoDescription = intent.getBooleanExtra("autoDescription", false);

                String[] pageTitleAndDescription = getPageTitleAndDescription(url);

                if (autoTitle){
                    this.loadedTitle = pageTitleAndDescription[0];
                }
                if (autoDescription){
                    this.loadedDescription = pageTitleAndDescription[1];
                }

                msg.arg1 = RETRIEVE_TITLE_ID;
                msg.obj = pageTitleAndDescription;
                // Send back messages to the calling activity
                try {
                    assert messenger != null;
                    messenger.send(msg);
                } catch (android.os.RemoteException | AssertionError e1) {
                    Log.w(getClass().getName(), "Exception sending message", e1);
                }
                break;
            default:
                // Do nothing
                break;
        }
    }

    /**
     * Display Toast in the main thread
     * Thanks : http://stackoverflow.com/a/3955826
     */
    private class DisplayToast implements Runnable{
        private final String mText;

        public DisplayToast(String text){
            mText = text;
        }

        public void run(){
            Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if the given credentials are correct
     * @param account The account with the credentials
     * @return NO_ERROR if nothing is wrong
     */
    private int checkShaarli(ShaarliAccount account){

        NetworkManager manager = new NetworkManager(account);
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
            NetworkManager manager = new NetworkManager(mShaarliAccount);
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
            sendNotificationShareError(sharedUrl, title, description, tags, privateShare);
        } else {
            mToastHandler.post(new DisplayToast(getString(R.string.add_success)));
            Log.i("SUCCESS", "Success while sharing link");
        }
    }

    /**
     * Retrieve the title of a page
     * @param url the page to get the title
     * @return the title page, "" if there is an error
     */
    private String[] getPageTitleAndDescription(String url){
        return NetworkManager.loadTitleAndDescription(url);
    }

    private void sendNotificationShareError(String sharedUrl, String title, String description, String tags, boolean privateShare){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Failed to share " + title)
                        .setContentText("Press to try again")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        // Creates an explicit intent To relaunch this service
        Intent resultIntent = new Intent(this, NetworkService.class);

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
