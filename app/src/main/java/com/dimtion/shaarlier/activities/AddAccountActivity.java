package com.dimtion.shaarlier.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.dimtion.shaarlier.R;
import com.dimtion.shaarlier.helpers.AccountsSource;
import com.dimtion.shaarlier.helpers.DebugHelper;
import com.dimtion.shaarlier.helpers.NetworkUtils;
import com.dimtion.shaarlier.services.NetworkService;
import com.dimtion.shaarlier.utils.ShaarliAccount;

import java.util.List;


public class AddAccountActivity extends AppCompatActivity {

    private String urlShaarli;
    private String username;
    private String password;
    private String restAPIKey;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String shortName;
    private ShaarliAccount account;
    private Boolean isDefaultAccount;
    private Boolean isValidateCert;
    private int authMethod;

    private Boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        Intent intent = getIntent();
        long accountId = intent.getLongExtra("_id", -1);

        if (accountId != -1) {
            isEditing = true;
            AccountsSource accountsSource = new AccountsSource(getApplicationContext());
            try {
                account = accountsSource.getShaarliAccountById(accountId);
            } catch (Exception e) { // Editing an account that does not exist, creating a new one
                account = null;
            }
            fillFields();
        } else {
            AccountsSource source = new AccountsSource(getApplicationContext());
            source.rOpen();
            List<ShaarliAccount> allAccounts = source.getAllAccounts();
            if (allAccounts.isEmpty()) {  // If it is the first account created
                CheckBox defaultCheck = findViewById(R.id.defaultAccountCheck);
                defaultCheck.setChecked(true);
                defaultCheck.setEnabled(false);
            }
        }
    }

    /**
    * Fill the fields with the selected account when editing a new account
    */
    private void fillFields() {
        // Get the user inputs :
        ((EditText) findViewById(R.id.urlShaarliView)).setText(account.getUrlShaarli());
        ((EditText) findViewById(R.id.usernameView)).setText(account.getUsername());
        ((EditText) findViewById(R.id.passwordView)).setText(account.getPassword());
        ((EditText) findViewById(R.id.shortNameView)).setText(account.getShortName());
        ((EditText) findViewById(R.id.restapiView)).setText(account.getRestAPIKey());

        ((Switch) findViewById(R.id.passwordAuthCheckbox)).setChecked(
                !(account.getRestAPIKey().length() > 0)
        );
        togglePasswordAuth(findViewById(R.id.passwordAuthCheckbox));

        if (!"".equals(account.getBasicAuthUsername())) {
            ((EditText) findViewById(R.id.basicUsernameView)).setText(account.getBasicAuthUsername());
            ((EditText) findViewById(R.id.basicPasswordView)).setText(account.getBasicAuthPassword());
            ((Switch) findViewById(R.id.basicAuthSwitch)).setChecked(true);
            enableBasicAuth(findViewById(R.id.basicAuthSwitch));
        }

        // default account?
        SharedPreferences prefs = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        this.isDefaultAccount = (prefs.getLong(getString(R.string.p_default_account), -1) == account.getId());
        ((CheckBox) findViewById(R.id.defaultAccountCheck)).setChecked(this.isDefaultAccount);

        findViewById(R.id.deleteAccountButton).setVisibility(View.VISIBLE);
    }

    /**
     * Action which handle the press on the try and save button
     *
     * @param view: needed for binding with interface actions
     */
    public void tryAndSaveAction(View view) {
        hideKeyboard();
        findViewById(R.id.tryingConfSpinner).setVisibility(View.VISIBLE);
        findViewById(R.id.tryConfButton).setVisibility(View.GONE);

        // Get the user inputs:
        final String urlShaarliInput = ((EditText) findViewById(R.id.urlShaarliView)).getText().toString();
        this.username = ((EditText) findViewById(R.id.usernameView)).getText().toString();
        this.password = ((EditText) findViewById(R.id.passwordView)).getText().toString();
        this.restAPIKey = ((EditText) findViewById(R.id.restapiView)).getText().toString();
        if (((Switch) findViewById(R.id.passwordAuthCheckbox)).isChecked()) {
            this.authMethod = ShaarliAccount.AUTH_METHOD_PASSWORD;
        } else {
            this.authMethod = ShaarliAccount.AUTH_METHOD_RESTAPI;
        }
        if (((Switch) findViewById(R.id.basicAuthSwitch)).isChecked()) {
            this.basicAuthUsername = ((EditText) findViewById(R.id.basicUsernameView)).getText().toString();
            this.basicAuthPassword = ((EditText) findViewById(R.id.basicPasswordView)).getText().toString();
        } else {
            this.basicAuthUsername = "";
            this.basicAuthPassword = "";
        }
        this.shortName = ((EditText) findViewById(R.id.shortNameView)).getText().toString();
        this.isDefaultAccount = ((CheckBox) findViewById(R.id.defaultAccountCheck)).isChecked();
        this.isValidateCert = !((CheckBox) findViewById(R.id.disableCertValidation)).isChecked();

        this.urlShaarli = NetworkUtils.toUrl(urlShaarliInput);

        ((EditText) findViewById(R.id.urlShaarliView)).setText(this.urlShaarli);  // Update the view

        // Depending on the
        // Create a fake account:
        ShaarliAccount accountToTest = new ShaarliAccount();
        accountToTest.setUrlShaarli(this.urlShaarli);
        accountToTest.setUsername(this.username);
        accountToTest.setPassword(this.password);
        accountToTest.setRestAPIKey(this.restAPIKey);
        accountToTest.setValidateCert(this.isValidateCert);
        accountToTest.setBasicAuthUsername(this.basicAuthUsername);
        accountToTest.setBasicAuthPassword(this.basicAuthPassword);
        accountToTest.setAuthMethod(this.authMethod);

        // Try the configuration
        Intent i = new Intent(this, NetworkService.class);
        i.putExtra("action", NetworkService.INTENT_CHECK);
        i.putExtra("account", accountToTest);
        i.putExtra(NetworkService.EXTRA_MESSENGER, new Messenger(new networkHandler(this, accountToTest)));
        startService(i);
    }

    /**
     * Handle the action of deletion: show a confirmation dialog then delete (if wanted)
     * @param view : The view needed for handling interface actions
     */
    public void deleteAccountAction(View view) {
        // Show dialog to confirm deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.text_confirm_deletion_account));
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAccount();
                finish();
            }
        });

        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Just dismiss the dialog
            }
        });
        builder.show();
    }

    /**
     * Delete the selected account from the database
     */
    private void deleteAccount() {
        AccountsSource source = new AccountsSource(getApplicationContext());
        source.wOpen();
        source.deleteAccount(this.account);
        source.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_validate) {
            tryAndSaveAction(findViewById(id));
        }

        return super.onOptionsItemSelected(item);
    }

    /**
    * Obviously hide the keyboard
     * From: http://stackoverflow.com/a/7696791/1582589
    */
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void enableBasicAuth(View toggle) {
        boolean checked = ((Switch) toggle).isChecked();
        findViewById(R.id.basicUsernameView).setEnabled(checked);
        findViewById(R.id.basicPasswordView).setEnabled(checked);
        findViewById(R.id.basicUsernameView).setVisibility(checked ? View.VISIBLE : View.GONE);
        findViewById(R.id.basicPasswordView).setVisibility(checked ? View.VISIBLE : View.GONE);
    }

    public void togglePasswordAuth(View toggle) {
        boolean checked = ((Switch) toggle).isChecked();
        findViewById(R.id.usernameView).setEnabled(checked);
        findViewById(R.id.usernameView).setVisibility(checked ? View.VISIBLE : View.GONE);

        findViewById(R.id.passwordView).setEnabled(checked);
        findViewById(R.id.passwordView).setVisibility(checked ? View.VISIBLE : View.GONE);

        findViewById(R.id.basicAuthSwitch).setEnabled(checked);
        findViewById(R.id.basicAuthSwitch).setVisibility(checked ? View.VISIBLE : View.GONE);

        findViewById(R.id.restapiView).setEnabled(!checked);
        findViewById(R.id.restapiView).setVisibility(!checked ? View.VISIBLE : View.GONE);
    }

    /**
     * Save a new account into the database,
     * should be called only if the account was verified.
     */
    private void saveAccount() {
        AccountsSource accountsSource = new AccountsSource(getApplicationContext());
        accountsSource.wOpen();
        // Since we do not yet save in the database the auth method, we rely on ShaarliAccount.AUTH_METHOD_AUTO
        // for publishing.
        // TODO: add to database saved auth method
        switch (authMethod) {
            case ShaarliAccount.AUTH_METHOD_PASSWORD:
                this.restAPIKey = "";
                break;
            case ShaarliAccount.AUTH_METHOD_RESTAPI:
                this.username = "";
                this.password = "";
                break;
        }

        try {
            if (isEditing) {  // Only update the database
                account.setUrlShaarli(this.urlShaarli);
                account.setUsername(this.username);
                account.setPassword(this.password);
                account.setBasicAuthUsername(this.basicAuthUsername);
                account.setBasicAuthPassword(this.basicAuthPassword);
                account.setShortName(this.shortName);
                account.setValidateCert(this.isValidateCert);
                account.setRestAPIKey(this.restAPIKey);
                accountsSource.editAccount(account);
            } else {
                this.account = accountsSource.createAccount(
                        this.urlShaarli,
                        this.username,
                        this.password,
                        this.basicAuthUsername,
                        this.basicAuthPassword,
                        this.shortName,
                        this.isValidateCert,
                        this.restAPIKey
                );
            }
        } catch (Exception e) {
            Log.e("ENCRYPTION ERROR", e.getMessage());
        } finally {
            accountsSource.close();
        }

        // Set the default account if needed
        if (this.isDefaultAccount) {
            SharedPreferences prefs = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putLong(getString(R.string.p_default_account), this.account.getId());
            editor.apply();
        }
    }

    private class networkHandler extends Handler {
        private final Activity mParent;
        private final ShaarliAccount mAccount;

        public networkHandler(Activity parent, ShaarliAccount account) {
            this.mParent = parent;
            this.mAccount = account;
        }

        /**
         * Handle the arrival of a message coming from the network service.
         *
         * @param msg the message given by the service
         */
        @Override
        public void handleMessage(Message msg) {
            findViewById(R.id.tryConfButton).setVisibility(View.VISIBLE);
            findViewById(R.id.tryingConfSpinner).setVisibility(View.GONE);

            // Show the returned error
            switch (msg.arg1) {
                case NetworkService.NO_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.success_test, Toast.LENGTH_LONG).show();
                    saveAccount();
                    finish();
                    break;
                case NetworkService.NETWORK_ERROR:
                    ((EditText) findViewById(R.id.urlShaarliView)).setError(getString(R.string.error_connecting));
                    enableSendReport((Exception) msg.obj);
                    break;
                case NetworkService.TOKEN_ERROR:
                    ((EditText) findViewById(R.id.urlShaarliView)).setError(getString(R.string.error_parsing_token));
                    enableSendReport(new Exception("TOKEN ERROR"));
                    break;
                case NetworkService.LOGIN_ERROR:
                    ((EditText) findViewById(R.id.usernameView)).setError(getString(R.string.error_login));
                    ((EditText) findViewById(R.id.passwordView)).setError(getString(R.string.error_login));
                    ((EditText) findViewById(R.id.restapiView)).setError(getString(R.string.error_login));
                    enableSendReport(new Exception("LOGIN ERROR"));
                    break;
                default:
                    ((EditText) findViewById(R.id.urlShaarliView)).setError(getString(R.string.error_unknown));
                    Toast.makeText(getApplicationContext(), R.string.error_unknown, Toast.LENGTH_LONG).show();
                    enableSendReport(new Exception("UNKNOWN ERROR"));
                    break;
            }
        }

        private void enableSendReport(final Exception error) {
            Button reportButton = findViewById(R.id.sendReportButton);
            reportButton.setVisibility(View.VISIBLE);
            reportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mParent);

                    builder.setMessage(R.string.report_issue).setTitle("REPORT - Shaarlier");

                    String extra = "Url Shaarli: " + urlShaarli + "\n";
                    extra += "Auth method id: " + mAccount.getAuthMethod() + "\n";
                    extra += "Validate cert: " + mAccount.isValidateCert() + "\n";
                    extra += "Basic auth: " + ((mAccount.getBasicAuthUsername().length() > 0) || (mAccount.getBasicAuthPassword().length() > 0)) + "\n";

                    final String finalExtra = extra;
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            DebugHelper.sendMailDev(
                                    mParent,
                                    "REPORT - Shaarlier",
                                    DebugHelper.generateReport(error, mParent, finalExtra)
                            );
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
    }
}