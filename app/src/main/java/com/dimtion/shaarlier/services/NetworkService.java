package com.dimtion.shaarlier.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.dimtion.shaarlier.R;
import com.dimtion.shaarlier.helpers.AccountsSource;
import com.dimtion.shaarlier.helpers.NetworkManager;
import com.dimtion.shaarlier.utils.Link;
import com.dimtion.shaarlier.utils.ShaarliAccount;

import java.io.IOException;

public class NetworkService extends IntentService {
    public static final String EXTRA_MESSENGER="com.dimtion.shaarlier.networkservice.EXTRA_MESSENGER";
    public static final int NO_ERROR = 0;
    public static final int NETWORK_ERROR = 1;
    public static final int TOKEN_ERROR = 2;
    public static final int LOGIN_ERROR = 3;

    public static final int RETRIEVE_TITLE_ID = 100;
    public static final int PREFETCH_LINK = 101;

    public static final int MANAGER_TIMEOUT = 60_000; // 60 secs for mobile connections

    public static final int INTENT_CHECK = 201;
    public static final int INTENT_POST = 202;
    public static final int INTENT_PREFETCH = 203;
    public static final int INTENT_RETRIEVE_TITLE_AND_DESCRIPTION = 204;

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
        int action = intent.getIntExtra("action", -1);

        switch (action) {
            case INTENT_CHECK:
                ShaarliAccount accountToTest = (ShaarliAccount) intent.getSerializableExtra("account");

                int shaarliLstatus = checkShaarli(accountToTest);
                Exception object = shaarliLstatus == NETWORK_ERROR ? mError : null;
                sendBackMessage(intent, shaarliLstatus, object);
                break;
            case INTENT_POST:
                String sharedUrl = intent.getStringExtra("sharedUrl");
                String title = intent.getStringExtra("title");
                String description = intent.getStringExtra("description");
                String tags = intent.getStringExtra("tags");
                boolean isPrivate = intent.getBooleanExtra("privateShare", true);
                boolean tweet = intent.getBooleanExtra("tweet", true);
                boolean toot = intent.getBooleanExtra("toot", true);

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
                    mShaarliAccount = (accountId != -1 ? acs.getShaarliAccountById(accountId) : acs.getDefaultAccount());
                } catch (Exception e) {
                    e.printStackTrace();
                    sendNotificationShareError(sharedUrl, title, description, tags, isPrivate, tweet, toot);
                }
                postLink(sharedUrl, title, description, tags, isPrivate, tweet, toot);
                stopSelf();
                break;
            case INTENT_PREFETCH:
                Link sharedLink = (Link) intent.getSerializableExtra("link");
                mShaarliAccount = sharedLink.getAccount();

                Link prefetchedLink = prefetchLink(sharedLink);

                sendBackMessage(intent, PREFETCH_LINK, prefetchedLink);

                break;

            case INTENT_RETRIEVE_TITLE_AND_DESCRIPTION:
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

                sendBackMessage(intent, RETRIEVE_TITLE_ID, pageTitleAndDescription);
                break;
            default:
                // Do nothing
                Log.e("NETWORK_ERROR", "Unknown intent action received: " + action);
                break;
        }
    }

    private void sendBackMessage(@NonNull Intent intent, int message_id, @Nullable Object message_content) {
        // Load the messenger to communicate back to the activity
        Messenger messenger = (Messenger) intent.getExtras().get(EXTRA_MESSENGER);
        Message msg = Message.obtain();

        msg.arg1 = message_id;
        msg.obj = message_content;
        try {
            assert messenger != null;
            messenger.send(msg);
        } catch (android.os.RemoteException | AssertionError e1) {
            Log.w(getClass().getName(), "Exception sending message", e1);
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

    /**
     * Try to prefetch the data of the link
     * Will return exactly the same link if the link does not exist
     * or if the prefetch failed.
     *
     * @param sharedLink partial link
     * @return new link with the prefetched data
     */
    private Link prefetchLink(Link sharedLink) {
        Link prefetchedLink = new Link(sharedLink);
        try {
            NetworkManager manager = new NetworkManager(sharedLink.getAccount());
            manager.setTimeout(MANAGER_TIMEOUT);

            if (manager.retrieveLoginToken() && manager.login()) {
                prefetchedLink = manager.prefetchLinkData(sharedLink);
            } else {
                mError = new Exception("Could not connect to the shaarli. Possibles causes : unhandled shaarli, bad username or password");
                Log.e("ERROR", mError.getMessage());
            }
        } catch (IOException | NullPointerException e) {
            mError = e;
            Log.e("ERROR", mError.getMessage());
        }
        return prefetchedLink;
    }


    private void postLink(String sharedUrl, String title, String description, String tags, boolean privateShare, boolean tweet, boolean toot){
        boolean posted = true;  // Assume it is shared
        try {
            // Connect the user to the site :
            NetworkManager manager = new NetworkManager(mShaarliAccount);
            manager.setTimeout(MANAGER_TIMEOUT);
            if(manager.retrieveLoginToken() && manager.login()) {
                manager.postLink(sharedUrl, title, description, tags, privateShare, tweet, toot);
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
            sendNotificationShareError(sharedUrl, title, description, tags, privateShare, tweet, toot);
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
    @NonNull
    private String[] getPageTitleAndDescription(String url){
        return NetworkManager.loadTitleAndDescription(url);
    }

    private void sendNotificationShareError(String sharedUrl, String title, String description, String tags, boolean privateShare, boolean tweet, boolean toot){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Failed to share " + title)
                        .setContentText("Press to try again")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        // Creates an explicit intent To relaunch this service
        Intent resultIntent = new Intent(this, NetworkService.class);

        resultIntent.putExtra("action", NetworkService.INTENT_POST);
        resultIntent.putExtra("sharedUrl", sharedUrl);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("tags", tags);
        resultIntent.putExtra("privateShare", privateShare);
        resultIntent.putExtra("tweet", tweet);
        resultIntent.putExtra("toot", toot);
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
