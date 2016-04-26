package com.dimtion.shaarlier;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class HttpSchemeHandlerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if(data != null) {
            String url = data.toString();

            Intent addActivityIntent = new Intent(this, AddActivity.class);
            addActivityIntent.setAction(Intent.ACTION_SEND);
            addActivityIntent.setType("text/plain");
            addActivityIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(addActivityIntent);
        } else {
            Toast.makeText(getApplicationContext(), R.string.add_not_handle, Toast.LENGTH_SHORT).show();
        }

        finish();
    }

}
