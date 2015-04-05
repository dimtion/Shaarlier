package com.dimtion.shaarlier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Make links clickable :
        ((TextView) findViewById(R.id.about_details)).setMovementMethod(LinkMovementMethod.getInstance());
        loadSettings();
        
    }

    @Override
   public void onPause(){
        super.onPause();
        saveSettings();
    }

    public void loginHandler(View view){
        String[] userInput = loadShaarliInput();
        
        // Is the URL possible ? :
        if (!NetworkManager.isUrl(userInput[0])) {
            Toast.makeText(getApplicationContext(), R.string.error_url, Toast.LENGTH_LONG).show();
        } else if (!NetworkManager.testNetwork(this)) { // Are we connected to the Internet ?
            Toast.makeText(getApplicationContext(), R.string.error_internet_connection, Toast.LENGTH_LONG).show();
        } else { // Obvious things are ok, let's try login in
            findViewById(R.id.isWorking).setVisibility(View.VISIBLE);
            new CheckShaarli().execute(userInput[0], userInput[1], userInput[2]);

            saveSettings();
        }
    }
    String[] loadShaarliInput(){
        EditText text;
        EditText urlInput = (EditText) findViewById(R.id.url_shaarli_input);
        String givenUrl = urlInput.getText().toString();

        text = (EditText) findViewById(R.id.username_input);
        String username = text.getText().toString();

        text = (EditText) findViewById(R.id.password_input);
        String password = text.getText().toString();

        Spinner protocolInput = (Spinner) findViewById(R.id.select_protocol);
        String protocol = protocolInput.getSelectedItem().toString();
        
        if (!givenUrl.equals("")) {
            // Edit the user url :
            if (!givenUrl.endsWith("/")) {
                givenUrl += '/';
            }

            if (givenUrl.startsWith("http://")) {
                givenUrl = givenUrl.replace("http://", "");
                protocolInput.setSelection(0, true); // Update protocol prompt
                protocol = "http://";
            } else if (givenUrl.startsWith("https://")) {
                givenUrl = givenUrl.replace("https://", "");
                protocolInput.setSelection(1, true); // Update protocol prompt
                protocol = "https://";
            }
        }
        // Update url prompt :
        urlInput.setText(givenUrl);

        final String shaarliUrl = protocol + givenUrl;
        
        return new String[]{shaarliUrl, username, password};
        
    }
    void saveSettings(){
        // Get user inputs :
        String url = ((EditText) findViewById(R.id.url_shaarli_input)).getText().toString();
        String username = ((EditText) findViewById(R.id.username_input)).getText().toString();
        String password = ((EditText) findViewById(R.id.password_input)).getText().toString();
        int protocol_id = ((Spinner) findViewById(R.id.select_protocol)).getSelectedItemPosition();
        String protocol = ((Spinner) findViewById(R.id.select_protocol)).getSelectedItem().toString();
        boolean isPrivate = ((CheckBox) findViewById(R.id.default_private)).isChecked();
        boolean isShareDialog = ((CheckBox) findViewById(R.id.show_share_dialog)).isChecked();
        boolean isAutoTitle = ((CheckBox) findViewById(R.id.auto_load_title)).isChecked();
        // Save data :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.p_url_shaarli), protocol + url)
                .putString(getString(R.string.p_user_url), url)
                .putString(getString(R.string.p_username), username)
                .putString(getString(R.string.p_password), password)
                .putInt(getString(R.string.p_protocol), protocol_id)
                .putBoolean(getString(R.string.p_default_private), isPrivate)
                .putBoolean(getString(R.string.p_show_share_dialog), isShareDialog)   
                .putBoolean(getString(R.string.p_auto_title), isAutoTitle)
                .apply();

    }
    void updateSettingsFromUpdate(SharedPreferences pref){
        int version = pref.getInt(getString(R.string.p_version),0);
        int currentVersion;
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e){
            currentVersion = 0;
        }
        if (version < currentVersion){
            String url = pref.getString(getString(R.string.p_url_shaarli), "");
            int protocol = 0;
            if(url.startsWith("http://")){
                url = url.replace("http://", "");
                protocol = 0;
            } else if (url.startsWith("https://")) {
                url = url.replace("https://", "");
                protocol = 1;
            }
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(getString(R.string.p_user_url), url)
                    .putInt(getString(R.string.p_protocol), protocol)
                    .apply();
        }
        
    }
    void loadSettings(){
        // Retrieve user previous settings
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        updateSettingsFromUpdate(pref);
        
        String url = pref.getString(getString(R.string.p_user_url), "");
        String usr = pref.getString(getString(R.string.p_username), "");
        String pwd = pref.getString(getString(R.string.p_password),"");
        int protocol = pref.getInt(getString(R.string.p_protocol), 0);
        boolean prv = pref.getBoolean(getString(R.string.p_default_private), false);
        boolean shrDiag = pref.getBoolean(getString(R.string.p_show_share_dialog), true);
        boolean isAutoTitle = pref.getBoolean(getString(R.string.p_auto_title), true);
        
        // Retrieve interface :
        EditText urlEdit = (EditText) findViewById(R.id.url_shaarli_input);
        EditText usernameEdit = (EditText) findViewById(R.id.username_input);
        EditText passwordEdit = (EditText) findViewById(R.id.password_input);
        CheckBox privateCheck = (CheckBox) findViewById(R.id.default_private);
        CheckBox shareDialogCheck = (CheckBox) findViewById(R.id.show_share_dialog);
        CheckBox autoTitleCheck = (CheckBox) findViewById(R.id.auto_load_title);
        Spinner protocolSelectSpinner = (Spinner) findViewById(R.id.select_protocol);

        // Init select_protocol spinner items :
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.select_protocol, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protocolSelectSpinner.setAdapter(adapter);

        // Display user previous settings :
        urlEdit.setText(url);
        usernameEdit.setText(usr);
        passwordEdit.setText(pwd);
        protocolSelectSpinner.setSelection(protocol);
        privateCheck.setChecked(prv);
        autoTitleCheck.setChecked(isAutoTitle);
        shareDialogCheck.setChecked(shrDiag);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_go_to_shaarli) {
            SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
            updateSettingsFromUpdate(pref);

            String url = pref.getString(getString(R.string.p_url_shaarli), getString(R.string.developer_shaarli));
            
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } else if (id == R.id.action_share) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle(getString(R.string.share));

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            input.setHint("http://www.perdu.com/");

            alert.setView(input);

            alert.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    Intent intent = new Intent(getBaseContext(), AddActivity.class);
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, value);
                    startActivity(intent);
                }
            });

            alert.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setValidated(Boolean value){
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.p_validated), value);
        editor.apply();
    }

    // Handle the test button
    private class CheckShaarli extends AsyncTask<String, Void, Integer> {

        // Error types :
        // 0 : no error
        // 1 : error connecting to shaarli
        // 2 : error parsing token
        // 3 : error login in

        @Override
        protected Integer doInBackground(String... urls) {
            NetworkManager manager = new NetworkManager(urls[0], urls[1], urls[2]);
            try {
                if (!manager.retrieveLoginToken()) {
                    return 2;
                }
                if (!manager.login()) {
                    return 3;
                }
            } catch (IOException e) {
                return 1;
            }
            return 0;
        }
    
        @Override
        protected void onPostExecute(Integer loginOutput) {
            if (loginOutput == 0) {
                // print success
                Toast.makeText(getApplicationContext(), R.string.success_test, Toast.LENGTH_LONG).show();
                // Save only on success :
                setValidated(true);
                saveSettings();
            } else if (loginOutput == 1) { // Error loading page
                setValidated(false);
                Toast.makeText(getApplicationContext(), R.string.error_connecting, Toast.LENGTH_LONG).show();
            } else if (loginOutput == 2) { // Error parsing token
                setValidated(false);
                Toast.makeText(getApplicationContext(), R.string.error_parsing_token, Toast.LENGTH_LONG).show();
            } else if (loginOutput == 3) { // Error login
                setValidated(false);
                Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_LONG).show();
            }
            findViewById(R.id.isWorking).setVisibility(View.GONE);
        }
        
    }
}