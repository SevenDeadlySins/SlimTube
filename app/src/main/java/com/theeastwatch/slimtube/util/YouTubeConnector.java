package com.theeastwatch.slimtube.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.theeastwatch.slimtube.R;

import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Provides an interface to Youtube, retrieving videos, search results, and (in the future) channel
 * and playlist information.
 */
public class YouTubeConnector {
    private YouTube youtube;
    private String apiKey;

    private static final String TAG = "YouTubeConnector";

    public YouTubeConnector (Context context) {
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }).setApplicationName(context.getString(R.string.app_name)).build();
        apiKey = context.getString(R.string.google_youtube_key);
    }

    public List<VideoItem> getVideos(List<String> videoIds) {
        List <VideoItem> videos = new ArrayList<VideoItem>();

        YouTube.Videos.List query = null;
        VideoListResponse response;
        try {
            query = youtube.videos().list("id,snippet,contentDetails");
            query.setId(TextUtils.join(", ", videoIds));
            query.setKey(apiKey);
//                query.setFields("items/contentDetails/duration");
        } catch (IOException e) {
            Log.d(TAG, "Could not initialize: " + e);
        }
        try {
            response = query.execute();
        } catch (IOException e) {
            Log.d(TAG, "IO Error: " + e);
            return videos;
        }

        for (Video video:response.getItems()) {
            PeriodFormatter formatter = ISOPeriodFormat.standard();
            Period p = formatter.parsePeriod(video.getContentDetails().getDuration());
            VideoItem videoItem = new VideoItem(video.getId(),
                    video.getSnippet().getTitle(),
                    video.getSnippet().getThumbnails().getMedium().getUrl(),
                    video.getSnippet().getChannelTitle(),
                    p.toStandardSeconds().getSeconds());
            videos.add(videoItem);
        }
        return videos;
    }

    public Search getSearch(String keywords, int maxResults) {
        return new Search(keywords, maxResults);
    }

    public class Search {
        private String keywords;
        private YouTube.Search.List query;
        private String nextToken = null;

        public Search(String keywords, int maxResults) {
            try {
                query = youtube.search().list("id,snippet");
                query.setQ(keywords);
                query.setKey(apiKey);
                query.setType("video");
                query.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/channelTitle),nextPageToken");
                query.setMaxResults((long) maxResults);
            } catch (IOException e) {
                Log.d(TAG, "Could not initialize: " + e);
            }
        }

        public List<VideoItem> getNextResults () {
            SearchListResponse response = null;
            List <VideoItem> videos = new ArrayList<VideoItem>();
            if (nextToken != null) {
                query.setPageToken(nextToken);
            }
            try {
                response = query.execute();
            } catch (IOException e) {
                Log.d(TAG, "IO Error: " + e);
                return videos;
            }
            StringBuilder videoIds = new StringBuilder();
            for (SearchResult result:response.getItems()) {
                VideoItem nextVideo = new VideoItem(result.getId().getVideoId(),
                        result.getSnippet().getTitle(),
                        result.getSnippet().getThumbnails().getDefault().getUrl(),
                        result.getSnippet().getChannelTitle(),
                        0);
                videoIds.append(result.getId().getVideoId());
                videoIds.append(',');
                videos.add(nextVideo);
            }
            YouTube.Videos.List durQuery = null;
            VideoListResponse durResponse;
            try {
                durQuery = youtube.videos().list("contentDetails");
                durQuery.setId(videoIds.toString());
                durQuery.setKey(apiKey);
//                query.setFields("items/contentDetails/duration");
            } catch (IOException e) {
                Log.d(TAG, "Could not initialize: " + e);
            }
            try {
                durResponse = durQuery.execute();
            } catch (IOException e) {
                Log.d(TAG, "IO Error: " + e);
                return videos;
            }
            List<Video> durations = durResponse.getItems();
            for (int i = 0; i < durations.size(); i++) {
                String dur = durations.get(i).getContentDetails().getDuration();
                PeriodFormatter formatter = ISOPeriodFormat.standard();
                Period p = formatter.parsePeriod(dur);

                videos.get(i).duration = p.toStandardSeconds().getSeconds();
            }
            nextToken = response.getNextPageToken();
            return videos;
        }
    }

/*
    public static class Channel {
        String channelTitle;
        String uploadsPlaylistId;
        String thumbnailUrl;
        String nextPageToken;

        public Channel (String userName) {

*/
/*
            StringBuilder channelInfoUrlString = new StringBuilder(API_BASE_URL);
            channelInfoUrlString.append("channels?part=snippet%2CcontentDetails&forUsername=");
            channelInfoUrlString.append(userName);
            channelInfoUrlString.append("fields=items(contentDetails%2Csnippet)&key=");
            channelInfoUrlString.append(R.string.google_youtube_key);
            String response = call(channelInfoUrlString.toString());
            try {
                JSONObject info = new JSONObject(response);
                JSONObject channelJSON = info.getJSONArray("items").getJSONObject(0);
                channelTitle = channelJSON.getJSONObject("snippet").getString("title");
                uploadsPlaylistId = channelJSON.getJSONObject("contentDetails").getJSONObject("relatedPlaylists").getString("uploads");
                thumbnailUrl = channelJSON.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("default").getString("url");
            }
            catch (JSONException e) {
                Log.e(TAG, "Exception during JSON parsing: " + Log.getStackTraceString(e), e);
            }
*//*

        }

        ArrayList<VideoItem> getnextVideos () {
*/
/*
            StringBuilder channelInfoUrlString = new StringBuilder(API_BASE_URL);
            channelInfoUrlString.append("playlistItems?part=contentDetails&maxResults=10&playlistId=");
            channelInfoUrlString.append(uploadsPlaylistId);
            if (!nextPageToken.equals("")) {
                channelInfoUrlString.append("&pageToken=");
                channelInfoUrlString.append(nextPageToken);
            }
            channelInfoUrlString.append("&fields=items(contentDetails)%2CnextPageToken&key=");
            channelInfoUrlString.append(R.string.google_youtube_key);
            try {
                JSONObject playlistJSON = new JSONObject(call(channelInfoUrlString.toString()));
                nextPageToken = playlistJSON.getString("nextPageToken");
                JSONArray playlistVideos = playlistJSON.getJSONArray("items");
                ArrayList<String> videoIds = new ArrayList<String>();
                for (int i = 0; i < playlistVideos.length(); i++) {
                    videoIds.add(playlistVideos.getJSONObject(i).getJSONObject("contentDetails").getString("videoId"));
                }
                ArrayList<PlaylistVideo> videos = new ArrayList<PlaylistVideo>();
                StringBuilder videoInfoUrlString = new StringBuilder(API_BASE_URL);
                videoInfoUrlString.append("videos?part=snippet%2CcontentDetails&id=");
                for (String s : videoIds) {
                    videoInfoUrlString.append(s);
                    videoInfoUrlString.append(',');
                }
                channelInfoUrlString.append("&key=");
                channelInfoUrlString.append(R.string.google_youtube_key);
                JSONArray videosJSON = new JSONObject(call(videoInfoUrlString.toString())).getJSONArray("items");
                for (int i = 0; i < videosJSON.length(); i++) {
                    videos.add(new PlaylistVideo(videosJSON.getJSONObject(i)));
                }
                return videos;
            }
            catch (JSONException e) {
                Log.e(TAG, "Exception during JSON parsing: " + Log.getStackTraceString(e), e);
                return null;
            }
        }
*//*

        }
    }
*/

}
