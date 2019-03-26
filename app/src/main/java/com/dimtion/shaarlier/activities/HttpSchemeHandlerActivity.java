package com.dimtion.shaarlier.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.dimtion.shaarlier.R;

public class HttpSchemeHandlerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if(data != null) {
            String url = data.toString();

            Intent addActivityIntent = new Intent(this, ShareActivity.class);
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
