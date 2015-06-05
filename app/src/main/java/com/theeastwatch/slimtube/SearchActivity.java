package com.theeastwatch.slimtube;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.theeastwatch.slimtube.util.EndlessScrollListener;
import com.theeastwatch.slimtube.util.VideoAdapter;
import com.theeastwatch.slimtube.util.VideoItem;
import com.theeastwatch.slimtube.util.YouTubeConnector;

import java.util.List;

/*
 * An activity that retrieves and displays search results.
 */
public class SearchActivity extends Activity {

    private String keywords = "";
    private TextView searchHeader;
    private ListView resultsList;
    private YouTubeConnector.Search search;
    private VideoAdapter adapter;

    private int NUM_RESULTS = 25;
    private static final String TAG = "SearchActivity";

    private class getNextResultsTask extends AsyncTask<Void, Void, List<VideoItem>> {
        @Override
        protected List<VideoItem> doInBackground(Void... params) {
            List<VideoItem> results = search.getNextResults();
            if (results.size() < NUM_RESULTS) {
                // There are no more results, kill the scroll listener.
                resultsList.setOnScrollListener(null);
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<VideoItem> videoItems) {
            super.onPostExecute(videoItems);
            //                videos.addAll(nextResults);
            adapter.addAll(videoItems);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handleIntent(getIntent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchHeader = (TextView) findViewById(R.id.search_header);
        searchHeader.setText("Results for \"" + keywords + "\"");
        YouTubeConnector connector = new YouTubeConnector(SearchActivity.this);
        search = connector.getSearch(keywords, NUM_RESULTS);
        resultsList = (ListView) findViewById(R.id.results_list);
        adapter = new VideoAdapter(this);
        resultsList.setAdapter(adapter);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoItem video = (VideoItem) adapter.getItem(position);
                Intent intent = new Intent(SearchActivity.this, PlayerActivity.class)
                        .putExtra("video_id", video.id)
                        .putExtra("video_title", video.title);
                startActivity(intent);
            }
        });
        resultsList.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void loadMore(int page, int totalItemCount) {
                new getNextResultsTask().execute();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            keywords = intent.getStringExtra(SearchManager.QUERY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
