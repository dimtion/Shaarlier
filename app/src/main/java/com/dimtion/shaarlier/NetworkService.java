package com.dimtion.shaarlier;

import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;

public class NetworkService extends IntentService {
    public static final String EXTRA_MESSENGER="com.dimtion.shaarlier.networkservice.EXTRA_MESSENGER";
    static final int NO_ERROR = 0;
    static final int NETWORK_ERROR = 1;
    static final int TOKEN_ERROR = 2;
    static final int LOGIN_ERROR = 3;
    private Exception mError;
    public NetworkService() {
        super("NetworkService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getStringExtra("action");
        if(action.equals("checkShaarli")){
            String urlShaarli = intent.getStringExtra("urlShaarli");
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");
            int result = checkShaarli(urlShaarli, username, password);
            Messenger messenger = (Messenger)intent.getExtras().get(EXTRA_MESSENGER);
            Message msg = Message.obtain();
            msg.arg1 = result;
            if(msg.arg1 == NETWORK_ERROR){
                msg.obj = mError;
            }
            try {
                assert messenger != null;
                messenger.send(msg);
            }
            catch (android.os.RemoteException | AssertionError e1) {
                Log.w(getClass().getName(), "Exception sending message", e1);

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
}
