package com.theeastwatch.slimtube.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/*
 * An adapter that displays an array of VideoItem objects.
 */
public class VideoAdapter extends ArrayAdapter<Object> {
    public VideoAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VideoItem item = (VideoItem) getItem(position);
        return item.toView(getContext(), convertView);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

}
