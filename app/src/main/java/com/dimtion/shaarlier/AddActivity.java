package com.dimtion.shaarlier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import java.io.IOException;


public class AddActivity extends Activity {
    private String urlShaarli;
    private String username;
    private String password;
    private Boolean privateShare;
    private boolean autoTitle;
    private boolean m_prefOpenDialog;

    private View a_dialogView;
    private AsyncTask a_TitleGetterExec;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_add);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // Check if the user had his data validated :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        urlShaarli = pref.getString(getString(R.string.p_url_shaarli), "");
        username = pref.getString(getString(R.string.p_username), "");
        password = pref.getString(getString(R.string.p_password), "");
        boolean vld = pref.getBoolean(getString(R.string.p_validated), false);
        privateShare = pref.getBoolean(getString(R.string.p_default_private), true);
        m_prefOpenDialog = pref.getBoolean(getString(R.string.p_show_share_dialog), false);
        autoTitle = pref.getBoolean(getString(R.string.p_auto_title), true);
        
        // convert urlShaarli into a real url :
        if(!urlShaarli.endsWith("/")){
            urlShaarli +='/';
        }
        if (!(urlShaarli.startsWith("http://") || urlShaarli.startsWith("https://"))){
            urlShaarli = "http://" + urlShaarli;
        }

        if(username.equals("") || password.equals("") || !vld){
            // If there is an error, launch the settings :
            Intent intentLaunchSettings = new Intent(this, MainActivity.class);
            startActivity(intentLaunchSettings);
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {

                ShareCompat.IntentReader reader = ShareCompat.IntentReader.from(this);
                String sharedUrl = reader.getText().toString();

                String sharedUrlTrimmed = this.extractUrl(sharedUrl);
                String defaultTitle = this.extractTitle(reader);

                // Show edit dialog if the users wants :
                if (m_prefOpenDialog) {
                    handleDialog(sharedUrlTrimmed, defaultTitle);
                } else {
                    if (autoTitle) {
                        final GetPageTitle getter = new GetPageTitle();
                        a_TitleGetterExec = getter.execute(sharedUrlTrimmed, defaultTitle);
                    }
                    new HandleAddUrl().execute(sharedUrlTrimmed, defaultTitle, "", "");
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.add_not_handle, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //
    // Method to extract the url from shared data and delete trackers
    //
    private String extractUrl(String sharedUrl) {
        String finalUrl;
        // trim the url because of annoying apps which send to much data :
        finalUrl = sharedUrl.trim();
        finalUrl = finalUrl.substring(finalUrl.lastIndexOf(" ") + 1);
        finalUrl = finalUrl.substring(finalUrl.lastIndexOf("\n") + 1);

        // If the url is incomplete :
        if (NetworkManager.isUrl("http://" + finalUrl) && !NetworkManager.isUrl(finalUrl)) {
            finalUrl = "http://" + finalUrl;
        }
        // Delete parameters added by trackers :
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

    //
    // Method to extract the title from shared data
    //
    private String extractTitle(ShareCompat.IntentReader reader) {
        String title;
        title = reader.getSubject() != null ? reader.getSubject() : "";
        if (title.contains(" ")) {
            title = title.substring(0, title.lastIndexOf(" "));
        } if (title.contains("\n")){
            title = title.substring(0, title.lastIndexOf("\n"));
        }

        return title;
    }

    //
    // Method made to handle the dialog box
    //
    private void handleDialog(final String sharedUrl, String givenTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));

        LayoutInflater inflater = AddActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.share_dialog, null);
        ((CheckBox) dialogView.findViewById(R.id.private_share)).setChecked(privateShare);
        this.a_dialogView = dialogView;

        // Load title :
        if (autoTitle && NetworkManager.isUrl(sharedUrl)) {
            loadAutoTitle(sharedUrl, givenTitle);
        }

        // Init tags :
        MultiAutoCompleteTextView textView = (MultiAutoCompleteTextView) dialogView.findViewById(R.id.tags);
        new AutoCompleteWrapper(textView, this);

        // Open the dialog :
        builder.setView(dialogView)
                .setTitle(R.string.share)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Retrieve interface :
                        String title = ((EditText) dialogView.findViewById(R.id.title)).getText().toString();
                        String description = ((EditText) dialogView.findViewById(R.id.description)).getText().toString();
                        String tags = ((EditText) dialogView.findViewById(R.id.tags)).getText().toString();
                        privateShare = ((CheckBox) dialogView.findViewById(R.id.private_share)).isChecked();

                        // In case sharing is too long, close keyboard:
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(dialogView.getWindowToken(), 0);


                        // Finally send everything
                        new HandleAddUrl().execute(sharedUrl, title, description, tags);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    // To get an automatic title :
    private void loadAutoTitle(String sharedUrl, String defaultTitle){
        a_dialogView.findViewById(R.id.loading_title).setVisibility(View.VISIBLE);
        ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.loading_title_hint);

        final GetPageTitle getter = new GetPageTitle();
        a_TitleGetterExec = getter.execute(sharedUrl, defaultTitle);
        ((EditText) a_dialogView.findViewById(R.id.title)).addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                getter.cancel(true);
                a_dialogView.findViewById(R.id.loading_title).setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        
    }
    private void updateTitle(String title, boolean isError){
        ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.title_hint);
        
        if(isError){
            ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.error_retrieving_title);
        } else if(!title.equals("")) {
            ((EditText) a_dialogView.findViewById(R.id.title)).setText(title);
        }

        a_dialogView.findViewById(R.id.loading_title).setVisibility(View.GONE);
    }
    
    //
    // Class which handle the arrival of a new share url, async
    //
    private class HandleAddUrl extends AsyncTask<String, Void, Boolean> {
        
        @Override
        protected Boolean doInBackground(String... url){

            // If there is no title, wait for title getter :
            String loadedTitle;
            String sharedTitle;
            if(url[1].equals("")){
                try {
                    loadedTitle = (String) a_TitleGetterExec.get();
                } catch (Exception e) { // could happen if the user didn't want to load titles.
                    loadedTitle = "";
                }
                sharedTitle = loadedTitle;
            } else {
                sharedTitle = url[1];
            }
            
            try {
                // Connect the user to the site :
                NetworkManager manager = new NetworkManager(urlShaarli, username, password);
                manager.retrieveLoginToken();
                manager.login();
                manager.postLink(url[0], sharedTitle, url[2], url[3], privateShare);
                
            } catch (IOException | NullPointerException e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean posted) {
            if (posted) {
                Toast.makeText(getApplicationContext(), R.string.add_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.add_error, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    private class GetPageTitle extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... url){
            if (url[1].equals("")) {
                return NetworkManager.loadTitle(url[0]);
            } else {
                return url[1];
            }
        }

        @Override
        protected void onPostExecute(String title) {
            if (m_prefOpenDialog) {
                if (title.equals("")) {
                    updateTitle(title, true);
                } else {
                    updateTitle(title, false);
                }
            }
        }
        
        @Override
        protected void onCancelled(String title){
            updateTitle("", false);
        }
    }
    }

    