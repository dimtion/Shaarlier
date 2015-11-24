package com.dimtion.shaarlier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ShareCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;


public class AddActivity extends Activity {
    private ShaarliAccount chosenAccount;
    private List<ShaarliAccount> allAccounts;
    private Boolean privateShare;
    private boolean autoTitle;
    private boolean autoDescription;
    private boolean stopLoadingTitle;
    private boolean stopLoadingDescription;
    private boolean m_prefOpenDialog;

    private View a_dialogView;

    private class networkHandler extends Handler {
        private final Activity mParent;

        public networkHandler(Activity parent) {
            this.mParent = parent;
        }

        /**
         * Handle the arrival of a message coming from the network service.
         *
         * @param msg the message given by the service
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case NetworkService.RETRIEVE_TITLE_ID:
                    if (m_prefOpenDialog) {
                        String title = ((String[]) msg.obj)[0];
                        String description = ((String[]) msg.obj)[1];

                        if(!stopLoadingTitle && autoTitle) {
                            if ("".equals(title)) {
                                updateTitle(title, true);
                            } else {
                                updateTitle(title, false);
                            }
                        }
                        if(!stopLoadingDescription && autoDescription) {
                            if ("".equals(description)) {
                                updateDescription(description, true);
                            } else {
                                updateDescription(description, false);
                            }
                        }
                    }
                    break;
                default:
                    Toast.makeText(getApplicationContext(), R.string.error_unknown, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_add);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // Get the user preferences :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        privateShare = pref.getBoolean(getString(R.string.p_default_private), true);
        m_prefOpenDialog = pref.getBoolean(getString(R.string.p_show_share_dialog), true);
        autoTitle = pref.getBoolean(getString(R.string.p_auto_title), true);
        autoDescription = pref.getBoolean(getString(R.string.p_auto_description), false);
        stopLoadingTitle = false;
        stopLoadingDescription = false;

        // Check if there is at least one account, if so launch the settings :
        getAllAccounts();
        if (this.allAccounts.isEmpty()) {
            Intent intentLaunchSettings = new Intent(this, MainActivity.class);
            startActivity(intentLaunchSettings);
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            ShareCompat.IntentReader reader = ShareCompat.IntentReader.from(this);
            String sharedUrl = reader.getText().toString();

            String sharedUrlTrimmed = this.extractUrl(sharedUrl);
            String defaultTitle = this.extractTitle(reader);

            String defaultDescription = intent.getStringExtra("description") != null ? intent.getStringExtra("description") : "";
            String defaultTags = intent.getStringExtra("tags") != null ? intent.getStringExtra("tags") : "";

            if (!autoTitle){
                defaultTitle = "";
            }
            if (!autoDescription){
                defaultDescription = "";
            }

            // Show edit dialog if the users wants
            if (m_prefOpenDialog) {
                handleDialog(sharedUrlTrimmed, defaultTitle, defaultDescription, defaultTags);
            } else {
                if (autoTitle || autoDescription) {
                    loadAutoTitleAndDescription(sharedUrlTrimmed, defaultTitle, defaultDescription);
                }
                handleSendPost(sharedUrlTrimmed, defaultTitle, defaultDescription, defaultTags, privateShare, this.chosenAccount);
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.add_not_handle, Toast.LENGTH_SHORT).show();
        }
    }

    //
    // Default account first
    //
    private void getAllAccounts() {
        AccountsSource accountsSource = new AccountsSource(this);
        accountsSource.rOpen();
        this.allAccounts = accountsSource.getAllAccounts();
        try {
            this.chosenAccount = accountsSource.getDefaultAccount();
        } catch (Exception e) {
            e.printStackTrace();
            this.chosenAccount = null;
        } finally {
            accountsSource.close();
        }

        if (this.chosenAccount != null) {
            int indexChosenAccount = 0;
            for (ShaarliAccount account : this.allAccounts) {
                if (account.getId() == this.chosenAccount.getId()) {
                    break;
                }
                indexChosenAccount++;
            }
            Collections.swap(this.allAccounts, indexChosenAccount, 0);
        }
    }

    //
    // Load the spinner for choosing the account
    //
    private void initAccountSpinner() {
        final Spinner accountSpinnerView = (Spinner) a_dialogView.findViewById(R.id.chooseAccount);
        ArrayAdapter adapter = new ArrayAdapter<>(this, R.layout.tags_list, this.allAccounts);
        accountSpinnerView.setAdapter(adapter);
        if (accountSpinnerView.getCount() <= 1) {
            accountSpinnerView.setVisibility(View.GONE);
        }
    }

    /**
     * Method to extract the url from shared data and delete trackers
     **/
    private String extractUrl(String sharedUrl) {
        String finalUrl;
        // trim the url because of annoying apps which send to much data :
        finalUrl = sharedUrl.trim();

        String[] possible_urls = finalUrl.split(" ");

        for (String url : possible_urls){
            if(NetworkManager.isUrl(url)){
                finalUrl = url;
                break;
            }
        }

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

    /**
     * Method to extract the title from shared data
     **/
    private String extractTitle(ShareCompat.IntentReader reader) {
        if (reader.getSubject() != null && !NetworkManager.isUrl(reader.getSubject())){
            return reader.getSubject();
        }

        return "";
    }

    //
    // Method made to handle the dialog box
    //
    private void handleDialog(final String sharedUrl, String givenTitle, String defaultDescription, String defaultTags) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));
        LayoutInflater inflater = AddActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.share_dialog, null);
        ((CheckBox) dialogView.findViewById(R.id.private_share)).setChecked(privateShare);
        this.a_dialogView = dialogView;

        // Init accountSpinner
        initAccountSpinner();

        // Load title or description:
        if ((autoDescription || autoTitle) && NetworkManager.isUrl(sharedUrl)) {
            loadAutoTitleAndDescription(sharedUrl, givenTitle, defaultDescription);
        }

        // Init url  :
        ((EditText) dialogView.findViewById(R.id.url)).setText(sharedUrl);

        // Init tags :
        MultiAutoCompleteTextView textView = (MultiAutoCompleteTextView) dialogView.findViewById(R.id.tags);
        ((EditText) dialogView.findViewById(R.id.tags)).setText(defaultTags);
        new AutoCompleteWrapper(textView, this);

        // Open the dialog :
        builder.setView(dialogView)
                .setTitle(R.string.share)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Retrieve user data
                        String url = ((EditText) dialogView.findViewById(R.id.url)).getText().toString();
                        String title = ((EditText) dialogView.findViewById(R.id.title)).getText().toString();
                        String description = ((EditText) dialogView.findViewById(R.id.description)).getText().toString();
                        String tags = ((EditText) dialogView.findViewById(R.id.tags)).getText().toString();
                        privateShare = ((CheckBox) dialogView.findViewById(R.id.private_share)).isChecked();
                        chosenAccount = (ShaarliAccount) ((Spinner) dialogView.findViewById(R.id.chooseAccount)).getSelectedItem();

                        // Finally send everything
                        handleSendPost(url, title, description, tags, privateShare, chosenAccount);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        })
                .show();
    }

    // To get an automatic title :
    private void loadAutoTitleAndDescription(String sharedUrl, String defaultTitle, String defaultDescription) {

        // Don't use network ressources if not needed
        if (!autoTitle && !autoDescription){
            return;
        }
        // Launch intent to retrieve the title and the description
        final Intent networkIntent = new Intent(this, NetworkService.class);
        networkIntent.putExtra("action", "retrieveTitleAndDescription");
        networkIntent.putExtra("url", sharedUrl);
        networkIntent.putExtra("autoTitle", autoTitle);
        networkIntent.putExtra("autoDescription", autoDescription);
        networkIntent.putExtra(NetworkService.EXTRA_MESSENGER, new Messenger(new networkHandler(this)));

        stopLoadingTitle = false;
        stopLoadingDescription = false;
        startService(networkIntent);

        // Everything is done in the NetworkService if no dialog is opened
        if (!m_prefOpenDialog){
            return;
        }

        if (autoDescription) {
            if ("".equals(defaultDescription)) {
                a_dialogView.findViewById(R.id.loading_description).setVisibility(View.VISIBLE);
                ((EditText) a_dialogView.findViewById(R.id.description)).setHint(R.string.loading_description_hint);

                // If in the meanwhile the user type text in the field, stop retrieving the description.
                ((EditText) a_dialogView.findViewById(R.id.description)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        stopLoadingDescription = true;
                        a_dialogView.findViewById(R.id.loading_description).setVisibility(View.GONE);
                        ((EditText) a_dialogView.findViewById(R.id.description)).removeTextChangedListener(this);
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Nothing to be done
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // Nothing to be done
                    }
                });
            } else {
                stopLoadingDescription = true;
                updateDescription(defaultDescription, false);
            }
        }

        if (autoTitle) {
            if ("".equals(defaultTitle)) {
                a_dialogView.findViewById(R.id.loading_title).setVisibility(View.VISIBLE);
                ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.loading_title_hint);
                // If in the meanwhile the user type text in the field, stop retrieving the title.
                ((EditText) a_dialogView.findViewById(R.id.title)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        stopLoadingTitle = true;
                        a_dialogView.findViewById(R.id.loading_title).setVisibility(View.GONE);
                        ((EditText) a_dialogView.findViewById(R.id.title)).removeTextChangedListener(this);
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Nothing to be done
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // Nothing to be done
                    }
                });
            } else {
                stopLoadingTitle = true;
                updateTitle(defaultTitle, false);
            }
        }
    }

    private void updateTitle(String title, boolean isError) {
        ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.title_hint);

        if (isError) {
            ((EditText) a_dialogView.findViewById(R.id.title)).setHint(R.string.error_retrieving_title);
        } else {
            ((EditText) a_dialogView.findViewById(R.id.title)).setText(title);
        }

        a_dialogView.findViewById(R.id.loading_title).setVisibility(View.GONE);
    }

    private void updateDescription(String description, boolean isError){
        ((EditText) a_dialogView.findViewById(R.id.description)).setHint(R.string.description_hint);

        if (isError) {
            ((EditText) a_dialogView.findViewById(R.id.description)).setHint(R.string.error_retrieving_description);
        } else {
            ((EditText) a_dialogView.findViewById(R.id.description)).setText(description);
        }

        a_dialogView.findViewById(R.id.loading_description).setVisibility(View.GONE);
    }

    /**
     * Start the network service to send the link with all its data to Shaarli
     * @param sharedUrl the one url
     * @param title a chosen title by the user
     * @param description user description
     * @param tags user tags
     * @param isPrivate true if the link is private
     * @param account the account which the share operate
     */
    private void handleSendPost(String sharedUrl, String title, String description, String tags, boolean isPrivate, ShaarliAccount account){
        Intent networkIntent = new Intent(this, NetworkService.class);
        networkIntent.putExtra("action", "postLink");
        networkIntent.putExtra("sharedUrl", sharedUrl);
        networkIntent.putExtra("title", title);
        networkIntent.putExtra("description", description);
        networkIntent.putExtra("tags", tags);
        networkIntent.putExtra("privateShare", isPrivate);
        networkIntent.putExtra("chosenAccountId", account.getId());
        networkIntent.putExtra(NetworkService.EXTRA_MESSENGER, new Messenger(new networkHandler(this)));

        startService(networkIntent);
        finish();
    }
}

    