package com.dimtion.shaarlier;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;


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
        accountsSource.close();

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
        boolean isHandlingHttpScheme = ((CheckBox) findViewById(R.id.handle_http_scheme)).isChecked();
        boolean useShaarli2twitter = ((CheckBox) findViewById(R.id.handle_twitter_plugin)).isChecked();

        // Save data :
        SharedPreferences pref = getSharedPreferences(getString(R.string.params), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.p_default_private), isPrivate)
                .putBoolean(getString(R.string.p_show_share_dialog), isShareDialog)
                .putBoolean(getString(R.string.p_auto_title), isAutoTitle)
                .putBoolean(getString(R.string.p_auto_description), isAutoDescription)
                .putBoolean(getString(R.string.p_shaarli2twitter), useShaarli2twitter)
                .apply();

        setHandleHttpScheme(isHandlingHttpScheme);
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
        boolean isHandlingHttpScheme = isHandlingHttpScheme();
        boolean isShaarli2twitter = pref.getBoolean(getString(R.string.p_shaarli2twitter), false);

        // Retrieve interface :
        CheckBox privateCheck = (CheckBox) findViewById(R.id.default_private);
        CheckBox shareDialogCheck = (CheckBox) findViewById(R.id.show_share_dialog);
        CheckBox autoTitleCheck = (CheckBox) findViewById(R.id.auto_load_title);
        CheckBox autoDescriptionCheck = (CheckBox) findViewById(R.id.auto_load_description);
        CheckBox handleHttpSchemeCheck = (CheckBox) findViewById(R.id.handle_http_scheme);
        CheckBox shaarli2twitter = (CheckBox) findViewById(R.id.handle_twitter_plugin);

        // Display user previous settings :
        privateCheck.setChecked(prv);
        autoTitleCheck.setChecked(isAutoTitle);
        autoDescriptionCheck.setChecked(isAutoDescription);
        handleHttpSchemeCheck.setChecked(isHandlingHttpScheme);
        shareDialogCheck.setChecked(sherDial);
        shaarli2twitter.setChecked(isShaarli2twitter);
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
        int id = item.getItemId();

        switch (id) {
            case R.id.action_share:
                shareDialog();
                break;
            case R.id.action_open_shaarli:
                openShaarliDialog();
                break;
            default:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.share));

        // TODO: move this to a xml file:
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
                Intent intent = new Intent(getBaseContext(), ShareActivity.class);
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

    private void openShaarliDialog() {
        AccountsSource accountsSource = new AccountsSource(this);
        accountsSource.rOpen();
        final List<ShaarliAccount> accounts = accountsSource.getAllAccounts();
        accountsSource.close();

        if (accounts == null || accounts.size() < 1) {
            return;
        }
        if (accounts.size() == 1) {
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(accounts.get(0).getUrlShaarli())
            );
            startActivity(browserIntent);
        }

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.action_open_shaarli));

        // TODO: move this to a xml file:
        final LinearLayout layout = new LinearLayout(this);
        for (final ShaarliAccount account : accounts) {
            Button b = new Button(this);
            b.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            b.setText(account.getShortName());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(account.getUrlShaarli())
                    );
                    startActivity(browserIntent);
                }
            });

            layout.addView(b);
        }

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 20, 20);

        alert.setView(layout);

        alert.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private boolean isHandlingHttpScheme() {
        return getPackageManager().getComponentEnabledSetting(getHttpSchemeHandlingComponent())
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void setHandleHttpScheme(boolean handleHttpScheme) {
        if(handleHttpScheme == isHandlingHttpScheme()) return;

        int flag = (handleHttpScheme ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

        getPackageManager().setComponentEnabledSetting(
                getHttpSchemeHandlingComponent(), flag, PackageManager.DONT_KILL_APP);
    }

    private ComponentName getHttpSchemeHandlingComponent() {
        return new ComponentName(this, HttpSchemeHandlerActivity.class);
    }

}