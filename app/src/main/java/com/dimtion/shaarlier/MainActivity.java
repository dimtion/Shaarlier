package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Map;


public class MainActivity extends ActionBarActivity {
    private Map<String, String> coockies;
    private String token;
    private String url_shaarli;
    private String username;
    private String password;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Retrieve user previous settings
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        String url = pref.getString(getString(R.string.p_url_shaarli), "http://");
        String usr = pref.getString(getString(R.string.p_username), "");
        String pwd = pref.getString(getString(R.string.p_password),"");
        Boolean prv = pref.getBoolean(getString(R.string.p_default_private), true);
        
        // Retrieve interface : 
        EditText urlEdit = (EditText) findViewById(R.id.url_shaarli_input);        
        EditText usernameEdit = (EditText) findViewById(R.id.username_input);        
        EditText passwordEdit = (EditText) findViewById(R.id.password_input);        
        CheckBox privateCheck = (CheckBox) findViewById(R.id.default_private);
        
        // Display user previous settings :
        urlEdit.setText(url);
        usernameEdit.setText(usr);
        passwordEdit.setText(pwd);
        privateCheck.setChecked(prv);
    }
    
    @Override
   public void onPause(){
        super.onPause();
        // Save settings :
        String url = ((EditText) findViewById(R.id.url_shaarli_input)).getText().toString();
        saveStringPref(R.string.p_url_shaarli, url);
        String usr = ((EditText) findViewById(R.id.username_input)).getText().toString();
        saveStringPref(R.string.p_username, usr);
        String pwd = ((EditText) findViewById(R.id.password_input)).getText().toString();
        saveStringPref(R.string.p_password, pwd);
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
            new CheckShaarli().execute(shaarliUrl, username, password);
            
            // On enregistre les crédits :
            SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(getString(R.string.p_url_shaarli), shaarliUrl)
                    .putString(getString(R.string.p_username), username)
                    .putString(getString(R.string.p_password), password);
            editor.apply();
        }
    }
    
    public void toogglePrivate(View view){
        boolean isChecked = ((CheckBox) findViewById(R.id.default_private)).isChecked();
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.p_default_private), isChecked);
        editor.apply();        
    }
    
    
    protected void saveStringPref(int key, String data){
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(key), data);
        editor.apply();        
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
    
    // TODO : compress CheckCredit and CheckShaarli in one class
    private class CheckCredit extends AsyncTask<String, Void, Boolean> { 
        
        @Override
        protected Boolean doInBackground(String... params) {
            final String loginUrl = url_shaarli;
            try {
                Connection.Response loginPage = Jsoup.connect(loginUrl)
                        .method(Connection.Method.POST)
                        .cookies(coockies)
                        .data("login", username)
                        .data("password", password)
                        .data("token", token)
                        .data("returnurl", url_shaarli)
                        .execute();
                
                Document document = loginPage.parse();
                Element logoutElement = document.body().select("a[href=?do=logout]").first();
                logoutElement.attr("href"); // If this fails, you're not connected
                coockies = loginPage.cookies();
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        
        @Override
        protected void onPostExecute(Boolean is_log_ok) {
            if (is_log_ok) {
                Toast.makeText(getApplicationContext(), R.string.success_test, Toast.LENGTH_LONG).show();
                
                // On enregistre ce succès :
                setValidated(true);

            } else {
                // TODO : préciser l'erreur pour l'utilisateur.
                Toast.makeText(getApplicationContext(), R.string.login_error, Toast.LENGTH_LONG).show();

                setValidated(false);
            }
        }
        
    }
    // Envoie une requete au Shaarli pour vérifier que s'en est bien un.
    private class CheckShaarli extends AsyncTask<String, Void, Boolean> {
        // private Exception exception;
        
        @Override
        protected Boolean doInBackground(String... urls) {
            final String loginFormUrl = urls[0] + "?do=login";
            try {
                // On récupère la page du formulaire :
                Connection.Response loginFormPage = Jsoup.connect(loginFormUrl)
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
                
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    
        @Override
        protected void onPostExecute(Boolean is_log_ok) {
            if (is_log_ok) {
                // Toast.makeText(getApplicationContext(), "Token : " + token, Toast.LENGTH_SHORT).show();
                // Maintenant qu'on a le token on vérifie que mdp-pseudo est correct.
                new CheckCredit().execute();
                
            } else {
                // TODO : préciser l'erreur pour l'utilisateur.
                Toast.makeText(getApplicationContext(), R.string.login_error, Toast.LENGTH_LONG).show();
                
                setValidated(false);
                
            }
        }
        
    }
    
}