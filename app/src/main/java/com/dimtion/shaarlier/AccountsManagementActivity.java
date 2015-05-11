package com.dimtion.shaarlier;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;


public class AccountsManagementActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_management);
        ListView accountsListView = (ListView) findViewById(R.id.accountListView);
        try {
            AccountsSource accountsSource = new AccountsSource(getApplicationContext());
            accountsSource.rOpen();
            List<ShaarliAccount> accountsList = accountsSource.getAllAccounts();

            ArrayAdapter<ShaarliAccount> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountsList);

            accountsListView.setAdapter(adapter);

            if (accountsList.isEmpty()) {
                TextView text = (TextView) findViewById(R.id.noAccountToShow);
                text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            }

            accountsSource.close();
        } catch (SQLException e) {
            Log.e("DB_ERROR", e.toString());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_accounts_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            Intent intent = new Intent(this, AddAccountActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}