package com.theeastwatch.slimtube.util;

import android.support.annotation.Nullable;
import android.util.Log;

import com.theeastwatch.slimtube.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Interface to the get_video_info API for Youtube.
 */
public class VideoInfoUtil {
    public static final String TAG = "VideoInfoUtil";
    private static final String API_VIDEO_INFO = "http://youtube.com/get_video_info/?el=player_embedded&video_id=";

    /*
     * Represents detailed playback information for a YT video.
     */
    public static class VideoInfo {
        public String title;
        public Integer duration;
        public ArrayList<Format> formats;
        public Boolean fail = false;
        public String failreason;

        public VideoInfo (String title, Integer duration, ArrayList<Format> formats){
            this.title = title;
            this.duration = duration;
            this.formats = formats;
        }

        /*
         * Creates a VideoInfo from a YouTube video ID.
         */
        public VideoInfo (String video_id) {
            Map <String, String> info = GetVideoInfo(video_id);
            if (info.get("status").equals("fail")) {
                fail = true;
                failreason = info.get("reason");
                return;
            }

            title = info.get("title");
            duration = Integer.parseInt(info.get("length_seconds"));
            formats = new ArrayList<Format>();

            String[] urls = info.get("url_encoded_fmt_stream_map").split(",");
            for (String url : urls) {
                formats.add(new Format(mapQuery(url)));
            }
//                maybe include this later for audio_only streams?
//                String[] fmts = info.get("adaptive_fmts").split(",");
//                for (String fmt : fmts) {
//                    System.out.println(mapQuery(fmt).toString());
//                }
        }
    }

    /*
     * Represents a format for playback. Contains the URL to the raw video as well as information
     * about the video file itself.
     */
    public static class Format {
        public Integer itag;
        public String type;
        public String format;
        public String quality;
        public String url;

        public Format (Integer itag, String type, String format, Integer bitrate, String quality, String url){
            this.itag = itag;
            this.type = type;
            this.format = format;
            this.quality = quality;
            this.url = url;
        }

        /*
         * Constructor for using the map from getVideoInfo to create a Format.
         */
        public Format (Map<String, String> fmt_info) {
            this.itag = Integer.parseInt(fmt_info.get("itag"));
            String[] split_type = fmt_info.get("type").split("/");
            this.type = split_type[0];
            this.format = split_type[1].split("; ")[0];
            this.quality = fmt_info.get("quality");
            this.url = fmt_info.get("url");
        }
    }

    /*
     * Calls a given URL and returns the response. Handles all I/O.
     */
    @Nullable
    public static String call(String toCall){
        HttpURLConnection urlConnection = null;
        String response = "";
        InputStream inStream = null;
        try {
            URL url = new URL(toCall);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            urlConnection.connect();
            inStream = urlConnection.getInputStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
            String temp = "";
            while ((temp = bReader.readLine()) != null) {
                response += temp;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception during HTTP request: " + Log.getStackTraceString(e), e);
            return null;
        }
        finally {
            if (inStream != null){
                try {
                    inStream.close();
                }
                catch (IOException ignored) {
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return response;
    }

    /*
     * Converts a string response to the get_video_info API call into a string map.
     */
    public static Map<String, String> GetVideoInfo (String videoId){
        StringBuilder videoInfoUrlString = new StringBuilder(API_VIDEO_INFO);
        videoInfoUrlString.append(videoId);
        Log.v(TAG, videoInfoUrlString.toString());
        String response = call(videoInfoUrlString.toString());
        Log.v(TAG, response);
        Map<String, String> info = null;
        info = mapQuery(response);
        return info;
    }

    /*
     * Converts a querystring into a map of strings.
     */
    @Nullable
    public static Map<String, String> mapQuery(String query) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error while decoding video info: " + Log.getStackTraceString(e), e);
                return null;
            }
        }
        return query_pairs;
    }

    public static void main(String [] args) {
        VideoInfo videoInfo = new VideoInfo("7zUcToIS2nQ");
        System.out.println("Title: " + videoInfo.title);
        System.out.println("Duration: " + videoInfo.duration / 60 + "m" + videoInfo.duration % 60 + "s");
        for (Format fmt : videoInfo.formats) {
            System.out.println("Itag " + fmt.itag + " - " + fmt.quality);
            System.out.println("Type: " + fmt.type + "/" + fmt.format);
            System.out.println("URL: " + fmt.url);
        }
    }
}
