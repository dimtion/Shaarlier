package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dimtion on 21/02/2015. 
 * Inspired from : http://stackoverflow.com/a/5051180
 * and : http://www.claytical.com/blog/android-dynamic-autocompletion-using-google-places-api
 */
class AutoCompleteWrapper {

    private final MultiAutoCompleteTextView a_textView;
    private final Context a_context;

    public AutoCompleteWrapper(final MultiAutoCompleteTextView textView, Context context) {
        this.a_textView = textView;
        this.a_context = context;

        this.a_textView.setTokenizer(new SpaceTokenizer());

        SharedPreferences pref = context.getSharedPreferences(context.getString(R.string.params), Context.MODE_PRIVATE);
        Set<String> tagsSet = pref.getStringSet(context.getString(R.string.saved_tags), new HashSet<String>());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.a_context, R.layout.tags_list);
        this.a_textView.setAdapter(adapter);
        this.a_textView.setThreshold(1);

        adapter.addAll(tagsSet);
        adapter.notifyDataSetChanged();

        AutoCompleteRetriever task = new AutoCompleteRetriever();
        task.execute();
    }

    private class AutoCompleteRetriever extends AsyncTask<String, Void, List<Tag>> {
        @Override
        protected List<Tag> doInBackground(String... foo) {
            AccountsSource accountsSource = new AccountsSource(a_context);
            accountsSource.rOpen();
            List<ShaarliAccount> accounts = accountsSource.getAllAccounts();

            TagsSource tagsSource = new TagsSource(a_context);
            tagsSource.wOpen();
            /* For the moment we keep all the tags, if later somebody wants to have the tags
            ** separated for each accounts, we will see
            */
            for (ShaarliAccount account : accounts) {
                // Download tags :
                NetworkManager manager = new NetworkManager(
                        account.getUrlShaarli(),
                        account.getUsername(),
                        account.getPassword());
                try {
                    manager.retrieveLoginToken();
                    manager.login();
                    String[] awesompleteTags = manager.retrieveTagsFromAwesomplete();
                    String[] wsTags = manager.retrieveTagsFromWs();  // Keep for compatibility
                    for (String tagValue : awesompleteTags) {
                        tagsSource.createTag(account, tagValue.trim());
                    }
                    for (String tagValue : wsTags) {
                        tagsSource.createTag(account, tagValue);
                    }
                } catch (IOException e) {
                    Log.e("ERROR", e.toString());
                }
            }
            List<Tag> tags = tagsSource.getAllTags();
            tagsSource.close();
            accountsSource.close();
            return tags;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<Tag> result) {
            ArrayAdapter<Tag> adapter = new ArrayAdapter<>(a_context, R.layout.tags_list, result);
            if (!result.isEmpty()){
                a_textView.setAdapter(adapter);
                adapter.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }
    }
}
