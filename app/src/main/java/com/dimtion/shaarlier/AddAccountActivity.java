package com.dimtion.shaarlier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;


public class AddAccountActivity extends ActionBarActivity {

    String urlShaarli;
    String username;
    String password;
    String shortName;
    ShaarliAccount account;

    Boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        Intent intent = getIntent();
        long accountId = intent.getLongExtra("_id", -1);

        if (accountId != -1) {
            isEditing = true;
            AccountsSource accountsSource = new AccountsSource(getApplicationContext());
            accountsSource.rOpen();
            account = accountsSource.getShaarliAccountById(accountId);
            accountsSource.close();
            fillFields();
        }
    }

    private void fillFields() {
        // Get the user inputs :
        ((EditText) findViewById(R.id.urlShaarliView)).setText(account.getUrlShaarli());
        ((EditText) findViewById(R.id.usernameView)).setText(account.getUsername());
        ((EditText) findViewById(R.id.passwordView)).setText(account.getPassword());
        ((EditText) findViewById(R.id.shortNameView)).setText(account.getShortName());

        findViewById(R.id.deleteAccountButton).setVisibility(View.VISIBLE);
    }

    public void deleteAccountAction(View view) {
        // Show dialog to be sure :
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this account ?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAccount();
                finish();
            }
        });

        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

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

    //
    // Obviously hide the keyboard
    // From : http://stackoverflow.com/a/7696791/1582589
    //
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void tryAndSaveAction(View view) {

        hideKeyboard();
        findViewById(R.id.tryingConfSpinner).setVisibility(View.VISIBLE);
        findViewById(R.id.tryConfButton).setVisibility(View.GONE);

        // Get the user inputs :
        final String urlShaarliInput = ((EditText) findViewById(R.id.urlShaarliView)).getText().toString();
        this.username = ((EditText) findViewById(R.id.usernameView)).getText().toString();
        this.password = ((EditText) findViewById(R.id.passwordView)).getText().toString();
        this.shortName = ((EditText) findViewById(R.id.shortNameView)).getText().toString();

        this.urlShaarli = NetworkManager.toUrl(urlShaarliInput);

        ((EditText) findViewById(R.id.urlShaarliView)).setText(this.urlShaarli);  // Update the view

        // Try the configuration :
        CheckShaarli checkShaarli = new CheckShaarli();
        checkShaarli.execute(this.urlShaarli, this.username, this.password);
    }

    //
    // Create a new account, should be called only if the login test passed successfully.
    //
    private void saveAccount() {
        AccountsSource accountsSource = new AccountsSource(getApplicationContext());
        accountsSource.wOpen();
        if (isEditing) {
            account.setUrlShaarli(this.urlShaarli);
            account.setUsername(this.username);
            account.setPassword(this.password);
            account.setShortName(this.shortName);
            accountsSource.editAccount(account);
        } else {
            accountsSource.createAccount(this.urlShaarli, this.username, this.password, this.shortName);
        }
        accountsSource.close();
    }

    // Tries the configuration on the web async
    private class CheckShaarli extends AsyncTask<String, Void, Integer> {

        static final int NO_ERROR = 0;
        static final int NETWORK_ERROR = 1;
        static final int TOKEN_ERROR = 2;
        static final int LOGIN_ERROR = 3;

        @Override
        protected Integer doInBackground(String... urls) {
            NetworkManager manager = new NetworkManager(urls[0], urls[1], urls[2]);
            try {
                if (!manager.retrieveLoginToken()) {
                    return TOKEN_ERROR;
                }
                if (!manager.login()) {
                    return LOGIN_ERROR;
                }
            } catch (IOException e) {
                return NETWORK_ERROR;
            }
            return NO_ERROR;
        }

        @Override
        protected void onPostExecute(Integer loginOutput) {

            findViewById(R.id.tryConfButton).setVisibility(View.VISIBLE);
            findViewById(R.id.tryingConfSpinner).setVisibility(View.GONE);

            switch (loginOutput) {
                case NO_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.success_test, Toast.LENGTH_LONG).show();
                    saveAccount();
                    finish();
                    break;
                case NETWORK_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.error_connecting, Toast.LENGTH_LONG).show();
                    break;
                case TOKEN_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.error_parsing_token, Toast.LENGTH_LONG).show();
                    break;
                case LOGIN_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}