package com.theeastwatch.slimtube.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.theeastwatch.slimtube.R;

/**
 * A class representing a YouTube video. Contains information on the ID, title, channel, duration,
 * and an URL for the thumbnail to display.
 */
public class VideoItem {
    public String id;
    public String title;
    public String thumbnailUrl;
    public String channelTitle;
    public Integer duration;

    public VideoItem(String id, String title, String thumbnailUrl, String channelTitle, Integer duration) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.channelTitle = channelTitle;
        this.duration = duration;
    }

    /*
     * A utility function that converts a VideoItem into a view for use in ListViews displaying
     * lists of videos. Converts a passed view or creates its own.
     */
    public View toView(Context context, View convertView) {
        View view = convertView;
        if (view == null) {
            int layoutId = R.layout.video_preview_layout;
            view = LayoutInflater.from(context).inflate(layoutId, null, false);
        }
        ImageView image = (ImageView) view.findViewById(R.id.video_thumbnail);
        Picasso.with(context).load(thumbnailUrl).into(image);
        ((TextView) view.findViewById(R.id.video_title)).setText(title);
        ((TextView) view.findViewById(R.id.video_channel_name)).setText(channelTitle);
        boolean hasHours = duration > 3600;

        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        StringBuilder timeDisplay = new StringBuilder();
        if (hasHours) {
            timeDisplay.append(hours);
            timeDisplay.append(':');
            timeDisplay.append(String.format("%02d", minutes));
        }
        else {
            timeDisplay.append(minutes);
        }
        timeDisplay.append(':');
        timeDisplay.append(String.format("%02d", seconds));

        ((TextView) view.findViewById(R.id.duration)).setText(timeDisplay.toString());

        return view;
    }
}
