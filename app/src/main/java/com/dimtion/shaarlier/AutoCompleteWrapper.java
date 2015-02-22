package com.dimtion.shaarlier;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by dimtion on 21/02/2015. 
 * Inspired from : http://stackoverflow.com/a/5051180 *
 * and : http://www.claytical.com/blog/android-dynamic-autocompletion-using-google-places-api *
 */
public class AutoCompleteWrapper {
    String siteUrl;
    ArrayAdapter<String> a_adapter;
    MultiAutoCompleteTextView a_textView;
    Context a_context;
    public AutoCompleteWrapper(final MultiAutoCompleteTextView textView, final ArrayAdapter<String> adapter, String url, Context context){
        siteUrl = url;
        a_adapter = adapter;
        a_textView = textView;
        a_context = context;
        adapter.setNotifyOnChange(true);
        textView.setAdapter(adapter);
        textView.setTokenizer(new SpaceTokenizer());
        
        textView.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.clear();
                AutoCompleteRetriever task = new AutoCompleteRetriever();
                //now pass the argument in the textview to the task
                Log.d("Shaarlier", "onTextChanged : " + textView.getText().toString());
                task.execute(textView.getText().toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            public void afterTextChanged(Editable s) {

            }
        });
    }
    
    class AutoCompleteRetriever extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            ArrayList<String> predictionsArr = new ArrayList<>();
            
            try {
                // Format the url :
                String data = URLEncoder.encode(urls[0], "UTF-8");
                final String requestUrl = siteUrl + "?ws=tags&term=" + data;
                
                String json = Jsoup.connect(requestUrl).ignoreContentType(true).execute().body();
                //json = "{ data :" + json + "}";
                // Convert response to array :
                //JSONObject predictions = new JSONObject(json);
                JSONArray ja = new JSONArray(json);
                for (int i = 0; i < ja.length(); i++) {
                    //add each entry to our array
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
            a_adapter = new ArrayAdapter<>(a_context, R.layout.tags_list);
            a_textView.setAdapter(a_adapter);
            for (String string : result) {
                Log.d("Shaarlier", "onPostExecute : result = " + string);
                
                // To get the last word :
                string = string.trim();
                String lastWord = string.substring(string.lastIndexOf(" ")+1);
                a_adapter.add(lastWord);
                a_adapter.notifyDataSetChanged();
            }
        }
    }
}
