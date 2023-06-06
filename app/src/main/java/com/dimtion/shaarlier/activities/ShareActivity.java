package com.dimtion.shaarlier.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dimtion.shaarlier.R;
import com.dimtion.shaarlier.dao.AccountsSource;
import com.dimtion.shaarlier.exceptions.UnsupportedIntent;
import com.dimtion.shaarlier.helper.AutoCompleteWrapper;
import com.dimtion.shaarlier.helper.ShareIntentParser;
import com.dimtion.shaarlier.models.Link;
import com.dimtion.shaarlier.models.ShaarliAccount;
import com.dimtion.shaarlier.network.NetworkUtils;
import com.dimtion.shaarlier.services.NetworkService;
import com.dimtion.shaarlier.utils.UserPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ShareActivity extends AppCompatActivity {

    static final int LOADER_TITLE = 0;
    static final int LOADER_DESCRIPTION = 1;
    static final int LOADER_PREFETCH = 2;
    private static final String LOGGER_NAME = ShareActivity.class.getSimpleName();
    private Link defaults;
    private UserPreferences userPrefs;
    private List<ShaarliAccount> accounts;

    private boolean isLoadingTitle = false;
    private boolean isLoadingDescription = false;
    private boolean isPrefetching = false;

    private boolean isNotNewLink = false;
    private Menu menu;

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        this.menu = menu;
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        userPrefs = UserPreferences.load(this);
        if (userPrefs.isOpenDialog()) {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        final List<ShaarliAccount> loadedAccounts = loadAccounts();
        if (loadedAccounts.isEmpty()) {
            Log.w(LOGGER_NAME, "No account configured, starting MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        this.accounts = loadedAccounts;
        final ShaarliAccount selectedAccount = loadedAccounts.get(0);

        final Intent intent = getIntent();
        final ShareIntentParser intentParser = new ShareIntentParser(selectedAccount, userPrefs);
        try {
            defaults = intentParser.parse(this, intent);
        } catch (final UnsupportedIntent e) {
            Toast.makeText(getApplicationContext(), R.string.add_not_handle, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (userPrefs.isOpenDialog()) {
            openDialog(defaults, userPrefs);
        } else {
            autoLoadTitleAndDescription(defaults);
            sendLink(defaults);
            finish();
        }
    }

    /**
     * Load user accounts and set the default one as the first of the list
     */
    private List<ShaarliAccount> loadAccounts() {
        final AccountsSource accountsSource = new AccountsSource(this);
        ShaarliAccount defaultAccount = null;
        List<ShaarliAccount> accounts = new ArrayList<>();
        accountsSource.rOpen();
        try {
            accounts = accountsSource.getAllAccounts();
            defaultAccount = accountsSource.getDefaultAccount();
        } catch (final Exception e) {
            Log.e(LOGGER_NAME, "Error while loading account sources");
            e.printStackTrace();
        } finally {
            accountsSource.close();
        }

        if (Objects.isNull(defaultAccount)) {
            return accounts;
        }

        // Put the default account first in the list
        int indexSelectedAccount = 0;
        for (final ShaarliAccount account : accounts) {
            if (account.getId() == defaultAccount.getId()) {
                break;
            }
            indexSelectedAccount++;
        }
        Collections.swap(accounts, indexSelectedAccount, 0);

        return accounts;
    }

    /**
     * Open a dialog for the user to change the description of the share
     */
    private void openDialog(final Link defaults, final UserPreferences userPrefs) {
        setContentView(R.layout.activity_share);

        initAccountSpinner();

        if (NetworkUtils.isUrl(defaults.getUrl())) {
            prefetchLink(defaults);
            autoLoadTitleAndDescription(defaults);
            updateLoadersVisibility();
        }

        ((EditText) findViewById(R.id.url)).setText(defaults.getUrl());

        final MultiAutoCompleteTextView textView = findViewById(R.id.tags);
        ((EditText) findViewById(R.id.tags)).setText(defaults.getTags());
        new AutoCompleteWrapper(textView, this);

        ((Checkable) findViewById(R.id.private_share)).setChecked(defaults.isPrivate());

        { // Init tweet if necessary
            final Switch tweetCheckBox = findViewById(R.id.tweet);
            tweetCheckBox.setChecked(userPrefs.isTweet());
            tweetCheckBox.setVisibility(userPrefs.isTweet() ? View.VISIBLE : View.GONE);
        }

        { // Init toot if necessary
            final Switch tootCheckBox = findViewById(R.id.toot);
            tootCheckBox.setChecked(userPrefs.isToot());
            tootCheckBox.setVisibility(userPrefs.isTweet() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Initiate a network handler to prefetch link information on Shaarli.
     * This can be used to know if a link was already saved, and then retrieve
     * its data.
     *
     * TODO: use this prefetching in order to save one network request when sharing
     * @param defaults defaults values
     */
    private void prefetchLink(@NonNull Link defaults) {
        final Intent networkIntent = new Intent(this, NetworkService.class);
        networkIntent.putExtra("action", NetworkService.INTENT_PREFETCH);
        networkIntent.putExtra("link", defaults);
        networkIntent.putExtra(NetworkService.EXTRA_MESSENGER, new Messenger(new NetworkHandler()));

        isPrefetching = true;
        startService(networkIntent);
    }

    private void initAccountSpinner() {
        final Spinner accountSpinner = this.findViewById(R.id.chooseAccount);
        ArrayAdapter<ShaarliAccount> adapter = new ArrayAdapter<>(this, R.layout.tags_list, accounts);
        accountSpinner.setAdapter(adapter);
        if (accountSpinner.getCount() < 2) {
            accountSpinner.setVisibility(View.GONE);
        }
        AccountSpinnerListener listener = new AccountSpinnerListener();
        accountSpinner.setOnItemSelectedListener(listener);
    }

    /**
     * Load everything from the interface and share the link
     */
    private void saveAndShare() {
        sendLink(linkFromUserInput());
        finish();
    }

    private void autoLoadTitleAndDescription(Link defaults) {
        // Don't use network resources if not needed
        if (!userPrefs.isAutoTitle() && !userPrefs.isAutoDescription()) {
            return;
        }
        // Launch intent to retrieve the title and the description
        final Intent networkIntent = new Intent(this, NetworkService.class);
        networkIntent.putExtra("action", NetworkService.INTENT_RETRIEVE_TITLE_AND_DESCRIPTION);
        networkIntent.putExtra("url", defaults.getUrl());
        networkIntent.putExtra("autoTitle", userPrefs.isAutoTitle());
        networkIntent.putExtra("autoDescription", userPrefs.isAutoDescription());
        networkIntent.putExtra(NetworkService.EXTRA_MESSENGER, new Messenger(new NetworkHandler()));

        isLoadingTitle = true;
        isLoadingDescription = true;
        startService(networkIntent);

        // Everything is done in the NetworkService if no dialog is opened
        if (!userPrefs.isOpenDialog()){
            return;
        }

        if (userPrefs.isAutoDescription()) {
            stopEarlyAutoLoad(
                    defaults.getDescription(),
                    R.id.loading_description,
                    R.id.description,
                    R.string.loading_description_hint,
                   LOADER_DESCRIPTION);
        }

        if (userPrefs.isAutoTitle()) {
            stopEarlyAutoLoad(
                    defaults.getTitle(),
                    R.id.loading_title,
                    R.id.title,
                    R.string.loading_title_hint,
                    LOADER_TITLE);
        }
    }

    /**
     * helper function for stopEarlyAutoload closure.
     * It is not very useful otherwise
     *
     * @param loaderId
     * @param value
     */
    private void setLoading(int loaderId, boolean value) {
        switch (loaderId) {
            case LOADER_TITLE:
                isLoadingTitle = value;
                break;
            case LOADER_DESCRIPTION:
                isLoadingDescription = value;
                break;
            case LOADER_PREFETCH:
                isPrefetching = value;
                break;
            default:
                break;
        }
    }

    private void updateLoadersVisibility() {
        View titleLoader = findViewById(R.id.loading_title);
        if (isLoadingTitle || isPrefetching) {
            titleLoader.setVisibility(View.VISIBLE);
        } else {
            titleLoader.setVisibility(View.GONE);
        }

        View descriptionLoader = findViewById(R.id.loading_description);
        if (isLoadingDescription || isPrefetching) {
            descriptionLoader.setVisibility(View.VISIBLE);
        } else {
            descriptionLoader.setVisibility(View.GONE);
        }
    }

    private void stopEarlyAutoLoad(String defaultValue, final int loader, final int field, int hint, final int loaderId) {
        if ("".equals(defaultValue)) {
            ((EditText) findViewById(field)).setHint(hint);

            // If in the meanwhile the user type text in the field, stop retrieving the data
            ((EditText) findViewById(field)).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    setLoading(loaderId, false);
                    findViewById(loader).setVisibility(View.GONE);
                    ((EditText) findViewById(field)).removeTextChangedListener(this);
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
            setLoading(loaderId, false);
            updateTitle(defaultValue, false);
        }
    }

    private void updateTitle(String title, boolean isError) {
        EditText titleEdit = findViewById(R.id.title);

        titleEdit.setHint(R.string.title_hint);

        if (isError) {
            titleEdit.setHint(R.string.error_retrieving_title);
        } else {
            titleEdit.setText(title);
        }

        updateLoadersVisibility();
    }

    private void updateDescription(String description, boolean isError) {
        EditText descriptionEdit = findViewById(R.id.description);
        descriptionEdit.setHint(R.string.description_hint);


        if (isError) {
            descriptionEdit.setHint(R.string.error_retrieving_description);
        } else {
            descriptionEdit.setText(description);
        }
        updateLoadersVisibility();
    }

    private void updateTags(String tags, boolean isError) {
        EditText tagsEdit = findViewById(R.id.tags);

        if (isError) {
            // Display nothing
            Log.e(LOGGER_NAME, "error retrieving tags");
        } else {
            tagsEdit.setText(tags);
        }
    }

    private void updatePrivate(boolean isPrivate) {
        Checkable tagsEdit = findViewById(R.id.private_share);
        tagsEdit.setChecked(isPrivate);
    }

    private void updateTweet(boolean tweet) {
        Checkable tweetCheck = findViewById(R.id.tweet);
        tweetCheck.setChecked(tweet);
    }

    private void updateToot(boolean toot) {
        Checkable tootCheck = findViewById(R.id.toot);
        tootCheck.setChecked(toot);
    }

    private Link linkFromUserInput() {
        return new Link(
                ((EditText) findViewById(R.id.url)).getText().toString(),
                ((EditText) findViewById(R.id.title)).getText().toString(),
                ((EditText) findViewById(R.id.description)).getText().toString(),
                ((EditText) findViewById(R.id.tags)).getText().toString(),
                ((Checkable) findViewById(R.id.private_share)).isChecked(),
                (ShaarliAccount) ((Spinner) findViewById(R.id.chooseAccount)).getSelectedItem(),
                ((Checkable) findViewById(R.id.tweet)).isChecked(),
                ((Checkable) findViewById(R.id.toot)).isChecked(),
                null,
                null
        );
    }

    class AccountSpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            setLoading(LOADER_DESCRIPTION, true);
            setLoading(LOADER_TITLE, true);
            updateLoadersVisibility();
            Link defaults = linkFromUserInput();
            // Override text values to avoid messing with `seemsNewLink`
            defaults.setTitle("");
            defaults.setDescription("");
            defaults.setTags("");

            prefetchLink(defaults);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // pass
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            saveAndShare();
        } else {
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    /**
     * Start the network service to send the link
     * @param link link to send
     */
    private void sendLink(@NonNull Link link) {
        Intent networkIntent = new Intent(this, NetworkService.class);
        networkIntent.putExtra("action", NetworkService.INTENT_POST);
        networkIntent.putExtra("link", link);
        networkIntent.putExtra(
                NetworkService.EXTRA_MESSENGER,
                new Messenger(new NetworkHandler())
        );

        startService(networkIntent);
    }

    private class NetworkHandler extends Handler {
        /**
         * Handle the arrival of a message coming from the network service.
         *
         * @param msg the message given by the service
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case NetworkService.RETRIEVE_TITLE_ID:
                    handleRetrieveTitleId(msg);
                    break;
                case NetworkService.PREFETCH_LINK:
                    handlePrefetchLink(msg);
                    break;
                default:
                    Toast.makeText(getApplicationContext(), R.string.error_unknown, Toast.LENGTH_LONG).show();
                    Log.e("NETWORK_MSG", "Unknown network intent received: " + msg.arg1);
                    break;
            }
        }

        private void handleRetrieveTitleId(final Message msg) {
            Log.i("NETWORK_MSG", "Title or description retrieved");
            if (userPrefs.isOpenDialog()) {
                final String title = ((String[]) msg.obj)[0];
                final String description = ((String[]) msg.obj)[1];

                if (isLoadingTitle && userPrefs.isAutoTitle()) {
                    updateTitle(title, "".equals(title));
                }
                if (isLoadingDescription && userPrefs.isAutoDescription()) {
                    updateDescription(description, "".equals(description));
                }
                setLoading(LOADER_TITLE, false);
                setLoading(LOADER_DESCRIPTION, false);
                updateLoadersVisibility();
            }
        }

        private void handlePrefetchLink(final Message msg) {
            Log.i("NETWORK_MSG", "Link prefetched");

            MenuItem isEditedIcon = menu.findItem(R.id.editing);
            setLoading(LOADER_PREFETCH, false);
            if (userPrefs.isOpenDialog()) {
                Link prefetchedLink = (Link) msg.obj;

                isNotNewLink = prefetchedLink.seemsNotNew();
                Log.i("PREFETCH_LINK", "SeemsNotNew? " + isNotNewLink);
                // Prefetching has priority over previously saved data since it
                // coming from Shaarli directly
                if (isNotNewLink) {
                    defaults = prefetchedLink;

                    // Update the interface
                    if (defaults.getTitle().length() > 0) {
                        updateTitle(defaults.getTitle(), false);
                    }
                    if (defaults.getDescription().length() > 0) {
                        updateDescription(defaults.getDescription(), false);
                    }
                    if (defaults.getTagList().size() > 0) {
                        updateTags(defaults.getTags(), false);
                    }
                    updatePrivate(defaults.isPrivate());
                    updateTweet(defaults.isTweet());
                    updateToot(defaults.isToot());

                    // Show that we are editing an existing entry
                    isEditedIcon.setVisible(true);
                } else {
                    isEditedIcon.setVisible(false);
                }
                // prefetch success: stop other loaders
                setLoading(LOADER_TITLE, false);
                setLoading(LOADER_DESCRIPTION, false);
                updateLoadersVisibility();
            }
        }
    }
}
