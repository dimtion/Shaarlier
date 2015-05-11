package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    private class AutoCompleteRetriever extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            ArrayList<String> tags = new ArrayList<>();

            // Get login data :
            SharedPreferences pref = a_context.getSharedPreferences(a_context.getString(R.string.params), Context.MODE_PRIVATE);
            String urlShaarli = pref.getString(a_context.getString(R.string.p_url_shaarli), "");
            String username = pref.getString(a_context.getString(R.string.p_username), "");
            String password = pref.getString(a_context.getString(R.string.p_password), "");

            // Download tags :
            NetworkManager manager = new NetworkManager(urlShaarli, username, password);
            try {
                manager.retrieveLoginToken();
                manager.login();
                String[] awesompleteTags = manager.retrieveTagsFromAwesomplete();
                String[] wsTags = manager.retrieveTagsFromWs();
                Collections.addAll(tags, awesompleteTags);
                Collections.addAll(tags, wsTags);
            } catch (IOException e) {
                return tags;
            }
            return tags;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<String> result) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(a_context, R.layout.tags_list);
            if (!result.isEmpty()){
                Set<String> tagsSet = new HashSet<>();
                for(String tag : result){
                    tag = tag.trim();
                    tagsSet.add(tag);
                }

                // If there is no in tags, no need to update the adapter :
                SharedPreferences pref = a_context.getSharedPreferences(a_context.getString(R.string.params), Context.MODE_PRIVATE);
                Set<String> savedTags = pref.getStringSet(a_context.getString(R.string.saved_tags), new HashSet<String>());
                if (!tagsSet.equals(savedTags)) {
                    // Show result :
                    a_textView.setAdapter(adapter);
                    adapter.addAll(tagsSet);
                    adapter.notifyDataSetChanged();
                }

                // Anyway : save the tags for next time :
                SharedPreferences.Editor editor = pref.edit();
                editor.putStringSet(a_context.getString(R.string.saved_tags), tagsSet);
                editor.apply();
            }
        }
    }
}
