package com.dimtion.shaarlier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by dimtion on 21/02/2015.
 * Inspired from : http://stackoverflow.com/a/5051180
 * and : http://www.claytical.com/blog/android-dynamic-autocompletion-using-google-places-api
 */
class AutoCompleteWrapper {

    private final MultiAutoCompleteTextView a_textView;
    private final Context a_context;
    private final ArrayAdapter<Tag> adapter;

    public AutoCompleteWrapper(final MultiAutoCompleteTextView textView, Context context) {
        this.a_textView = textView;
        this.a_context = context;

        this.a_textView.setTokenizer(new SpaceTokenizer());

        this.adapter = new ArrayAdapter<>(a_context, R.layout.tags_list);
        this.a_textView.setAdapter(this.adapter);
        this.a_textView.setThreshold(1);
        updateTagsView();

        AutoCompleteRetriever task = new AutoCompleteRetriever();
        task.execute();
    }

    private void updateTagsView() {
        try {
            TagsSource tagsSource = new TagsSource(a_context);
            tagsSource.rOpen();
            List<Tag> tagList = tagsSource.getAllTags();
            tagsSource.close();

            this.adapter.clear();
            this.adapter.addAll(tagList);
            this.adapter.notifyDataSetChanged();

            this.a_textView.setAdapter(this.adapter);

        } catch (Exception e){
            sendReport(e);
        }
    }

    private class AutoCompleteRetriever extends AsyncTask<String, Void, Boolean> {
        private Exception mError;
        @Override
        protected Boolean doInBackground(String... foo) {
            AccountsSource accountsSource = new AccountsSource(a_context);
            accountsSource.rOpen();
            List<ShaarliAccount> accounts = accountsSource.getAllAccounts();

            Boolean success = true;
            /* For the moment we keep all the tags, if later somebody wants to have the tags
            ** separated for each accounts, we will see
            */
            for (ShaarliAccount account : accounts) {
                // Download tags :
                NetworkManager manager = new NetworkManager(account);
                TagsSource tagsSource = new TagsSource(a_context);
                try {
                    if(manager.retrieveLoginToken() && manager.login()) {
                        String[] awesompleteTags = manager.retrieveTagsFromAwesomplete();
                        String[] wsTags = manager.retrieveTagsFromWs();  // Keep for compatibility
                        if (awesompleteTags == null && wsTags == null) {
                            mError = manager.getLastError();
                            success = false;
                        } else {
                            tagsSource.wOpen();
                            if(awesompleteTags!= null) {
                                for (String tagValue : awesompleteTags) {
                                    tagsSource.createTag(account, tagValue.trim());
                                }
                            }
                            if(wsTags != null) {
                                for (String tagValue : wsTags) {
                                    tagsSource.createTag(account, tagValue);
                                }
                            }
                            tagsSource.close();
                        }
                    } else {
                        mError = new Exception("Could not login");
                        success = false;
                    }

                } catch (Exception e) {
                    mError = e;
                    success = false;
                    Log.e("ERROR", e.toString());
                } finally {
                    tagsSource.close();
                }
            }

            accountsSource.close();
            return success;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Boolean r) {
            if(!r) {
                String error = (mError != null) ? mError.getMessage() : "";
                Toast.makeText(a_context, a_context.getString(R.string.error_retrieving_tags) + " -- " + error, Toast.LENGTH_LONG).show();
            } else {
                updateTagsView();
            }
        }
    }

    private void sendReport(final Exception error) {
        final Activity activity = (Activity) a_context;
        AlertDialog.Builder builder = new AlertDialog.Builder(a_context);

        builder.setMessage("Would you like to report this issue ?").setTitle("REPORT - Shaarlier: add link");


        final String extra = ""; // "Url Shaarli: " + account.getUrlShaarli();

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                DebugHelper.sendMailDev(activity, "REPORT - Shaarlier: load tags", DebugHelper.generateReport(error, activity, extra));
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
