package com.dimtion.shaarlier;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by dimtion on 21/02/2015. 
 * Inspired from : http://stackoverflow.com/a/5051180 *
 */
public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private ArrayList<String> mData;

    public AutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mData = new ArrayList<String>();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public String getItem(int index) {
        return mData.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter myFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // A class that queries a web API, parses the data and returns an ArrayList<Style>
                    StyleFetcher fetcher = new StyleFetcher();
                    try {
                        mData = fetcher.retrieveResults(constraint.toString());
                    } catch (Exception e) {
                        Log.e("myException", e.getMessage());
                    }
                    // Now assign the values and count to the FilterResults object
                    filterResults.values = mData;
                    filterResults.count = mData.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence contraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return myFilter;
    }
}
class AutoCompleteRetriever extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... urls) {
        // params comes from the execute() call: params[0] is the url.
        try {
            return downloadUrl(urls[0]);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }
    
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        textView.setText(result);
    }
}