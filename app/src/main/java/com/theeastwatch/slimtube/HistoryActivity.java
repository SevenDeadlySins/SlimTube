package com.theeastwatch.slimtube;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.theeastwatch.slimtube.util.EndlessScrollListener;
import com.theeastwatch.slimtube.util.HistoryDBHelper;
import com.theeastwatch.slimtube.util.VideoAdapter;
import com.theeastwatch.slimtube.util.VideoItem;
import com.theeastwatch.slimtube.util.YouTubeConnector;

import java.util.List;

/*
 * An activity for accessing the History DB and displaying its contents.
 */
public class HistoryActivity extends Activity {

    private VideoAdapter adapter;
    private YouTubeConnector connector;
    private String lastVideo = null;
    private ListView historyList;

    private HistoryDBHelper dbHelper;

    private class getNextResultsTask extends AsyncTask<Void, Void, List<VideoItem>> {
        @Override
        protected List<VideoItem> doInBackground(Void... params) {
            List<String> videoIds =  dbHelper.getAllVideos(lastVideo, 25, true);
            if (videoIds.size() > 0) {
                // sanity check, don't try to access video -1 if there are no videos
                lastVideo = videoIds.get(videoIds.size() - 1);
            }
            if (videoIds.size() > 25) {
                // there are no more videos after this set, no need to load more
                historyList.setOnScrollListener(null);
            }
            return connector.getVideos(videoIds);
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        connector = new YouTubeConnector(this);
        dbHelper = new HistoryDBHelper(this);
        historyList = (ListView) findViewById(R.id.history_list);
        adapter = new VideoAdapter(this);
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoItem video = (VideoItem) adapter.getItem(position);
                Intent intent = new Intent(HistoryActivity.this, PlayerActivity.class)
                        .putExtra("video_id", video.id)
                        .putExtra("video_title", video.title);
                startActivity(intent);
            }
        });
        historyList.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void loadMore(int page, int totalItemCount) {
                new getNextResultsTask().execute();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
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
