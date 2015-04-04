package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dimtion on 21/02/2015. 
 * Inspired from : http://stackoverflow.com/a/5051180
 * and : http://www.claytical.com/blog/android-dynamic-autocompletion-using-google-places-api
 */
public class AutoCompleteWrapper {
    String siteUrl;

    MultiAutoCompleteTextView a_textView;
    Context a_context;
    public AutoCompleteWrapper(final MultiAutoCompleteTextView textView, String url, Context context){
        siteUrl = url;
        a_textView = textView;
        a_context = context;

        a_textView.setTokenizer(new SpaceTokenizer());

        SharedPreferences pref = context.getSharedPreferences(context.getString(R.string.params), Context.MODE_PRIVATE);
        Set<String> tagsSet = pref.getStringSet(context.getString(R.string.saved_tags), new HashSet<String>());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(a_context, R.layout.tags_list);
        a_textView.setAdapter(adapter);
        a_textView.setThreshold(1);

        adapter.addAll(tagsSet);
        adapter.notifyDataSetChanged();

        AutoCompleteRetriever task = new AutoCompleteRetriever();
        task.execute();
    }
    
    class AutoCompleteRetriever extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            ArrayList<String> predictionsArr = new ArrayList<>();

            try {
                // Format the url :
//                String data = URLEncoder.encode(urls[0], "UTF-8");
                final String requestUrl = siteUrl + "?ws=tags&term=+";
                
                String json = Jsoup.connect(requestUrl).ignoreContentType(true).execute().body();

                JSONArray ja = new JSONArray(json);
                for (int i = 0; i < ja.length(); i++) {
                    // add each entry to our array
                    predictionsArr.add(ja.getString(i));
                }
                
            } catch (IOException | JSONException e) {
               Log.d("Shaarlier", e.getMessage());
            }
            return predictionsArr;
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
