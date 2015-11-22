package com.dimtion.shaarlier;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;


public class AccountsManagementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_management);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final ListView accountsListView = (ListView) findViewById(R.id.accountListView);
        AccountsSource accountsSource = new AccountsSource(getApplicationContext());
        try {
            accountsSource.rOpen();
            List<ShaarliAccount> accountsList = accountsSource.getAllAccounts();
            ArrayAdapter<ShaarliAccount> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountsList);

            accountsListView.setAdapter(adapter);

            if (accountsList.isEmpty())
                findViewById(R.id.noAccountToShow).setVisibility(View.VISIBLE);
            else
                findViewById(R.id.noAccountToShow).setVisibility(View.GONE);


        } catch (SQLException e) {
            Log.e("DB_ERROR", e.toString());
        } finally {
            accountsSource.close();
        }
        accountsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                ShaarliAccount clickedAccount = (ShaarliAccount) accountsListView.getItemAtPosition(position);

                addOrEditAccount(clickedAccount);
            }
        });
    }

    private void addOrEditAccount(ShaarliAccount account) {
        Intent intent = new Intent(this, AddAccountActivity.class);
        if (account != null) {
            intent.putExtra("_id", account.getId());
            Log.w("EDIT ACCOUNT", account.getShortName());
        }
        startActivity(intent);
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
            addOrEditAccount(null);
        }

        return super.onOptionsItemSelected(item);
    }
}