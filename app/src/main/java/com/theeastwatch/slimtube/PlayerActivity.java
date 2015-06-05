package com.theeastwatch.slimtube;

import com.theeastwatch.slimtube.util.HistoryDBHelper;
import com.theeastwatch.slimtube.util.VideoInfoUtil;
import com.theeastwatch.slimtube.util.PlayerUtil;
import com.theeastwatch.slimtube.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PlayerActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        QualityDialogFragment.QualitySelectedListener {

    private String contentId;
    private VideoInfoUtil.VideoInfo videoInfo;

    private boolean hasHours;
    private boolean hasActiveHolder = false;

    private static final String TAG = "PlayerActivity";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = false;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Below are objects used in onCreate.
     */
    private ActionBar actionBar;
    private Handler handler;

    private View controlsView;
//    private View shutterView;
    private ProgressBar loadSpinner;
    private ImageButton playButton;
    private SeekBar seekBar;
    private TextView timeDisplayView;
    private TextView totalTimeDisplayView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private MediaPlayer player;
    private boolean playerNeedsPrepare;

    private ArrayList<String> qualities;
    private int playerPosition = 0;
    private int selectedFormat;
    private boolean limitMobileData;

    /**
     * At one point in the future, may support audio only streams.
     */
    private boolean audioOnly;

    public PlayerActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_player);

        // PlayerActivity can be the first access of the app, so set default preferences.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        limitMobileData = sharedPref.getBoolean("pref_limit_mobile_data", false);

        handler = new Handler();
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, data.toString());
            contentId = data.getHost().equals("youtu.be") ?
                    data.getLastPathSegment() : data.getQueryParameter("v");
        }
        else {
            contentId = intent.getStringExtra("video_id");
        }

        Log.d(TAG, contentId);
        new FetchVideoInfoTask().execute(contentId);

        actionBar = getActionBar();
        View root = findViewById(R.id.root);
        timeDisplayView = (TextView) findViewById(R.id.time_display);
        totalTimeDisplayView = (TextView) findViewById(R.id.total_time_display);

        controlsView = findViewById(R.id.fullscreen_content_controls);
        surfaceView = (SurfaceView) findViewById(R.id.fullscreen_content);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        playButton = (ImageButton) findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null){
                    if (player.isPlaying()) {
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                    else {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                    }
                }
                else {
                    preparePlayer();
                }
            }
        });
//        loadSpinner = (ProgressBar) findViewById(R.id.load_spinner);
//        shutterView = findViewById(R.id.shutter);

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && fromUser) {
                    player.pause();
                    player.seekTo(progress * 1000);
                    setTimeDisplay(progress);
                    player.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, surfaceView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible) {
                            actionBar.show();
                        }
                        else {
                            actionBar.hide();
                        }
                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.play_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.seekbar).setOnTouchListener(mDelayHideTouchListener);
    }

    private class FetchVideoInfoTask extends AsyncTask<String, Void, VideoInfoUtil.VideoInfo> {
        @Override
        protected VideoInfoUtil.VideoInfo doInBackground(String... ids) {
            return new VideoInfoUtil.VideoInfo(ids[0]);
        }

        protected void onPostExecute(VideoInfoUtil.VideoInfo result){
            videoInfo = result;
            if (result.fail) {
                Toast.makeText(PlayerActivity.this, result.failreason, Toast.LENGTH_LONG).show();
                return;
            }
            HistoryDBHelper helper = new HistoryDBHelper(PlayerActivity.this);
            helper.addVideo(contentId, videoInfo.title);

            actionBar.setTitle(result.title);
            qualities = new ArrayList<String>();
            for (VideoInfoUtil.Format fmt : result.formats) {
                qualities.add(fmt.quality + " - " + fmt.format);
            }
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            Log.d(TAG, mWifi.isConnected() ? "wifi connected" : "wifi not connected");
            if (limitMobileData && !mWifi.isConnected()) {
                Log.d(TAG, "Limiting mobile data, selecting first non-hd format");
                for (int i = 0; i < qualities.size(); i++){
                    Log.d(TAG, "Checking " + qualities.get(i));
                    if (!qualities.get(i).toLowerCase().contains("hd")){
                        Log.d(TAG, "No HD here, selecting " + i);
                        selectedFormat = i;
                        break;
                    }
                }
            }
            else {
                Log.d(TAG, "No need to limit mobile data, selecting first stream.");
                selectedFormat = 0;
            }
            Log.d(TAG, "Selected format " + selectedFormat + ", " + qualities.get(selectedFormat));

            hasHours = result.duration > 3600;

            int hours = result.duration / 3600;
            int minutes = (result.duration % 3600) / 60;
            int seconds = result.duration % 60;
            StringBuilder timeDisplay = new StringBuilder();
            if (hasHours) {
                timeDisplay.append(hours);
                timeDisplay.append(':');
            }
            timeDisplay.append(String.format("%02d", minutes));
            timeDisplay.append(':');
            timeDisplay.append(String.format("%02d", seconds));

            totalTimeDisplayView.setText(timeDisplay.toString());

            preparePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player == null && videoInfo != null) {
            preparePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!audioOnly) {
            releasePlayer();
        }
//        shutterView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        if (view == playButton) {
            preparePlayer();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_quality) {
            if (qualities == null) {
                // don't blow up if qualities haven't been loaded yet
                Toast.makeText(this, R.string.toast_quality_unavailable, Toast.LENGTH_SHORT);
            }
            else {
                DialogFragment dialog = QualityDialogFragment.newInstance(qualities, selectedFormat);
                dialog.show(getFragmentManager(), "QualityDialogFragment");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setTimeDisplay(int position) {
        int hours = position / 3600;
        int minutes = (position % 3600) / 60;
        int seconds = position % 60;
        StringBuilder timeDisplay = new StringBuilder();
        if (hasHours) {
            timeDisplay.append(hours);
            timeDisplay.append(':');
        }
        timeDisplay.append(String.format("%02d", minutes));
        timeDisplay.append(':');
        timeDisplay.append(String.format("%02d", seconds));

        timeDisplayView.setText(timeDisplay.toString());

    }

    private void preparePlayer() {
        Log.d(TAG, "in preparePlayer");
        try {
            if (player == null) {
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setOnPreparedListener(this);
                player.setDataSource(videoInfo.formats.get(selectedFormat).url);
                if (hasActiveHolder) { player.setDisplay(surfaceView.getHolder()); }
                playerNeedsPrepare = true;
                Log.d(TAG, "Player created, ready for prepare.");
            }
        }
        catch (IOException e) {

        }
        if (playerNeedsPrepare) {
            Log.d(TAG, "Player needs prepare, preparing.");
            player.prepareAsync();
            playerNeedsPrepare = false;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
        }
    }


    // MediaPlayer listener implementations

    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "prepared");
//        shutterView.setVisibility(View.GONE);
        if (playerPosition > 0) {
            player.seekTo(playerPosition);
        }
        seekBar.setMax(mp.getDuration() / 1000);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    int currentPosition = player.getCurrentPosition() / 1000;
                    seekBar.setProgress(currentPosition);
                    setTimeDisplay(currentPosition);
                }
                handler.postDelayed(this, 1000);
            }
        });
        player.start();
        playButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    public void onCompletion(MediaPlayer mp) {
        // reset player position to 0 in case video is played again.
        playerPosition = 0;
        // set up the play button
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Player encountered an error.", Toast.LENGTH_LONG).show();
        playerNeedsPrepare = true;
        return false;
    }

    // OnQualitySelected listener implementation.

    public void onQualitySelected(int selected) {
        selectedFormat = selected;
        releasePlayer();
        preparePlayer();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setDisplay(holder);
        }
        hasActiveHolder = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasActiveHolder = false;
    }
}
