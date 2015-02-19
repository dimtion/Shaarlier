package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Make links clickable :
        ((TextView) findViewById(R.id.about_details)).setMovementMethod(LinkMovementMethod.getInstance());
        // Retrieve user previous settings
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        String url = pref.getString(getString(R.string.p_url_shaarli), "http://");
        String usr = pref.getString(getString(R.string.p_username), "");
        String pwd = pref.getString(getString(R.string.p_password),"");
        boolean prv = pref.getBoolean(getString(R.string.p_default_private), true);
        boolean shrDiag = pref.getBoolean(getString(R.string.p_show_share_dialog), true);

        // Retrieve interface :
        EditText urlEdit = (EditText) findViewById(R.id.url_shaarli_input);
        EditText usernameEdit = (EditText) findViewById(R.id.username_input);
        EditText passwordEdit = (EditText) findViewById(R.id.password_input);
        CheckBox privateCheck = (CheckBox) findViewById(R.id.default_private);
        CheckBox shareDialogCheck = (CheckBox) findViewById(R.id.show_share_dialog);

        // Display user previous settings :
        urlEdit.setText(url);
        usernameEdit.setText(usr);
        passwordEdit.setText(pwd);
        privateCheck.setChecked(prv);
        shareDialogCheck.setChecked(shrDiag);
    }

    @Override
   public void onPause(){
        super.onPause();
        saveSettings();
    }

    public void loginHandler(View view){

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        EditText text;
        text = (EditText) findViewById(R.id.url_shaarli_input);
        String given_url = text.getText().toString();
        text = (EditText) findViewById(R.id.username_input);
        String username = text.getText().toString();
        text = (EditText) findViewById(R.id.password_input);
        String password = text.getText().toString();

        if(!given_url.endsWith("/")){
            given_url +='/';
        }
        if (!(given_url.startsWith("http://") || given_url.startsWith("https://"))){
            given_url = "http://" + given_url;
        }
        final String shaarliUrl = given_url;

        // Is the URL possible ? :
        if (!URLUtil.isValidUrl(shaarliUrl)) {
            Toast.makeText(getApplicationContext(), R.string.error_url, Toast.LENGTH_LONG).show();
        } else if (networkInfo == null || !networkInfo.isConnected()) { // Are we connected to internet ?
            Toast.makeText(getApplicationContext(), R.string.error_internet_connection, Toast.LENGTH_LONG).show();
        } else {
            // Si il n'y a pas d'erreur d'input on vérifie que les crédits sont corrects :
            findViewById(R.id.isWorking).setVisibility(View.VISIBLE);
            new CheckShaarli().execute(shaarliUrl, username, password);

            // On enregistre les crédits :
            saveSettings();
        }
    }

    void saveSettings(){
        // Get user inputs :
        String url = ((EditText) findViewById(R.id.url_shaarli_input)).getText().toString();
        String username = ((EditText) findViewById(R.id.username_input)).getText().toString();
        String password = ((EditText) findViewById(R.id.password_input)).getText().toString();
        boolean isPrivate = ((CheckBox) findViewById(R.id.default_private)).isChecked();
        boolean isShareDialog = ((CheckBox) findViewById(R.id.show_share_dialog)).isChecked();
        // Save data :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.p_url_shaarli), url)
                .putString(getString(R.string.p_username), username)
                .putString(getString(R.string.p_password), password)
                .putBoolean(getString(R.string.p_default_private), isPrivate)
                .putBoolean(getString(R.string.p_show_share_dialog), isShareDialog)
                .apply();

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
        }

        return super.onOptionsItemSelected(item);
    }

    private void setValidated(Boolean value){
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.p_validated), value);
        editor.apply();
    }

    // Envoie une requete au Shaarli pour vérifier que s'en est bien un.
    private class CheckShaarli extends AsyncTask<String, Void, Boolean> {

        // Errors types :
        // 0 : no error
        // 1 : error connecting to shaarli
        // 2 : error parsing token
        // 3 : error login in
        private int error;

        @Override
        protected Boolean doInBackground(String... urls) {
            this.error = 0;
            final String loginFormUrl = urls[0] + "?do=login";
            // Get a token and check if the shaarli exists
            Map<String, String> coockies;
            String token;
            String url_shaarli;
            String username;
            String password;
            try {
                // On récupère la page du formulaire :
                Connection.Response loginFormPage = Jsoup.connect(loginFormUrl)
                        .followRedirects(true)
                        .method(Connection.Method.GET)
                        .execute();
                Document loginPageDoc = loginFormPage.parse();
                Element tokenElement = loginPageDoc.body().select("input[name=token]").first();

                // On conserve les cookies et le token :
                coockies = loginFormPage.cookies();
                token = tokenElement.attr("value");
                url_shaarli = urls[0];
                username = urls[1];
                password = urls[2];
            } catch (IOException   e) {
                this.error = 1;
                return false;
            } catch (NullPointerException e) {
                this.error = 2;
                return false;
            }

            final String loginUrl = url_shaarli;
            // Attempt login in :
            try {
                Connection.Response loginPage = Jsoup.connect(loginUrl)
                        .method(Connection.Method.POST)
                        .followRedirects(true)
                        .cookies(coockies)
                        .data("login", username)
                        .data("password", password)
                        .data("token", token)
                        .data("returnurl", url_shaarli)
                        .execute();

                Document document = loginPage.parse();
                Element logoutElement = document.body().select("a[href=?do=logout]").first();
                logoutElement.attr("href"); // If this fails, you're not connected
            } catch (IOException   e) {
                this.error = 1;
                return false;
            } catch (NullPointerException e) {
                this.error = 3;
                return false;
            }
            return true;
        }
    
        @Override
        protected void onPostExecute(Boolean is_log_ok) {
            if (is_log_ok) {
                // print success
                Toast.makeText(getApplicationContext(), R.string.success_test, Toast.LENGTH_LONG).show();
                // Save the success :
                setValidated(true);
                
            } else {
                if(this.error == 1) { // Error loading page
                    Toast.makeText(getApplicationContext(), R.string.error_connecting, Toast.LENGTH_LONG).show();
                } else if(this.error == 2) { // Error parsing token
                    Toast.makeText(getApplicationContext(), R.string.error_parsing_token, Toast.LENGTH_LONG).show();
                } else if (this.error == 3){ // Error login
                    Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_LONG).show();
                }
                setValidated(false);
            }
            findViewById(R.id.isWorking).setVisibility(View.GONE);
        }
        
    }
    
}