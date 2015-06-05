package com.theeastwatch.slimtube.util;

import android.widget.AbsListView;

/**
 * A listener for maintaining endless scroll on a ListView.
 */
public abstract class EndlessScrollListener implements AbsListView.OnScrollListener {
    private int bufferItemCount = 5;
    private int currentPage = 0;
    private int previousTotal = 0;
    private boolean loading = false;

    public EndlessScrollListener() {}

    public EndlessScrollListener(int bufferItemCount) {
        this.bufferItemCount = bufferItemCount;
    }

    /* Implemented on instantiation, this is called when more entries are needed. */
    public abstract void loadMore(int page, int totalItemCount);

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (loading) {
            if (totalItemCount > previousTotal) {
                // we've finished loading, update item count / page / status
                loading = false;
                previousTotal = totalItemCount;
                currentPage++;
            }
        }
        if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + bufferItemCount)) {
            // Start loading when we can see less items than the set buffer count.
            loadMore(currentPage + 1, totalItemCount);
            loading = true;
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Do nothing
    }
}
