package com.dimtion.shaarlier;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private boolean m_isNoAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make links clickable :
        ((TextView) findViewById(R.id.about_details)).setMovementMethod(LinkMovementMethod.getInstance());

        loadSettings();

        // Load custom design :
        TextView textVersion = (TextView) findViewById(R.id.text_version);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            textVersion.setText(String.format(getString(R.string.version), versionName));

        } catch (PackageManager.NameNotFoundException e) {
            textVersion.setText(getText(R.string.text_version));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountsSource accountsSource = new AccountsSource(getApplicationContext());
        accountsSource.rOpen();
        m_isNoAccount = accountsSource.getAllAccounts().isEmpty();

        Button manageAccountsButton = (Button) findViewById(R.id.button_manage_accounts);
        if (m_isNoAccount) {
            manageAccountsButton.setText(R.string.add_account);
        } else {
            manageAccountsButton.setText(R.string.button_manage_accounts);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveSettings();
    }

    private void saveSettings() {
        // Get user inputs :
        boolean isPrivate = ((CheckBox) findViewById(R.id.default_private)).isChecked();
        boolean isShareDialog = ((CheckBox) findViewById(R.id.show_share_dialog)).isChecked();
        boolean isAutoTitle = ((CheckBox) findViewById(R.id.auto_load_title)).isChecked();
        boolean isAutoDescription = ((CheckBox) findViewById(R.id.auto_load_description)).isChecked();
        // Save data :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.p_default_private), isPrivate)
                .putBoolean(getString(R.string.p_show_share_dialog), isShareDialog)
                .putBoolean(getString(R.string.p_auto_title), isAutoTitle)
                .putBoolean(getString(R.string.p_auto_description), isAutoDescription)
                .apply();

    }

    public void openAccountsManager(View view) {
        Intent intent;
        if (m_isNoAccount) {
            intent = new Intent(this, AddAccountActivity.class);
        } else {
            intent = new Intent(this, AccountsManagementActivity.class);

        }
        startActivity(intent);

    }

    private void loadSettings() {
        // Retrieve user previous settings
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
//        updateSettingsFromUpdate(pref);

        boolean prv = pref.getBoolean(getString(R.string.p_default_private), false);
        boolean sherDial = pref.getBoolean(getString(R.string.p_show_share_dialog), true);
        boolean isAutoTitle = pref.getBoolean(getString(R.string.p_auto_title), true);
        boolean isAutoDescription = pref.getBoolean(getString(R.string.p_auto_description), false);

        // Retrieve interface :
        CheckBox privateCheck = (CheckBox) findViewById(R.id.default_private);
        CheckBox shareDialogCheck = (CheckBox) findViewById(R.id.show_share_dialog);
        CheckBox autoTitleCheck = (CheckBox) findViewById(R.id.auto_load_title);
        CheckBox autoDescriptionCheck = (CheckBox) findViewById(R.id.auto_load_description);

        // Display user previous settings :
        privateCheck.setChecked(prv);
        autoTitleCheck.setChecked(isAutoTitle);
        autoDescriptionCheck.setChecked(isAutoDescription);
        shareDialogCheck.setChecked(sherDial);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (m_isNoAccount) {
            menu.findItem(R.id.action_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            menu.findItem(R.id.action_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_share:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(getString(R.string.share));

                // TODO : move this to a xml file :
                final LinearLayout layout = new LinearLayout(this);

                final TextView textView = new TextView(this);
                if (Build.VERSION.SDK_INT < 23) {
                    //noinspection deprecation
                    textView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                } else {
                    textView.setTextAppearance(android.R.style.TextAppearance_Medium);
                }
                textView.setText(getText(R.string.text_new_url));

                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                input.setHint(getText(R.string.hint_new_url));

                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(10, 10, 20, 20);

                layout.addView(textView);
                layout.addView(input);
                alert.setView(layout);

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
                break;
            default:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}