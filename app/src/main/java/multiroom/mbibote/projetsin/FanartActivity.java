package multiroom.mbibote.projetsin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class FanartActivity extends AppCompatActivity implements FanartManager.OnFanartCacheChangeListener{
    private static final String TAG = FanartActivity.class.getSimpleName();

    private static final String STATE_ARTWORK_POINTER = "artwork_pointer";
    private static final String STATE_ARTWORK_POINTER_NEXT = "artwork_pointer_next";
    private static final String STATE_LAST_TRACK = "last_track";


    private static final int FANART_SWITCH_TIME =12 * 1000;

    private TextView mTrackTitle;
    private TextView mTrackAlbum;
    private TextView mTrackArtist;

    private MPDTrack mLastTrack;

    private ServersStatusListener mStateListerner=null;

    private ViewSwitcher mSwitcher;
    private Timer mSwitchTimer;

    private int mNextFanart;
    private int mCurrentFanart;

    private ImageView mFanartView0;
    private ImageView mFanartView1;

    private ImageButton mPlayPauseButton;

    private SeekBar mPositionSeekbar;


    private SeekBar mVolumeSeekbar;
    private ImageView mVolumeIcon;
    private ImageView mVolumeIconButtons;

    private TextView mVolumeText;

    private VolumeButtonLongClickListener mPlusListener;
    private VolumeButtonLongClickListener mMinusListener;

    private LinearLayout mVolumeSeekbarLayout;
    private LinearLayout mVolumeButtonLayout;

    private int mVolumeStepSize = 1;

    private FanartManager mFanartManager;

    View mDecorView;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();

        setContentView(R.layout.activity_artist_fanart);

        mTrackTitle = findViewById(R.id.textview_track_title);
        mTrackAlbum = findViewById(R.id.textview_track_album);
        mTrackArtist = findViewById(R.id.textview_track_artist);

        mSwitcher = findViewById(R.id.fanart_switcher);

        mFanartView0 = findViewById(R.id.fanart_view_0);
        mFanartView1 = findViewById(R.id.fanart_view_1);

        final ImageButton previousButton = findViewById(R.id.button_previous_track);
        previousButton.setOnClickListener(v -> MPDCommandHandler.previousSong());

        final ImageButton nextButton = findViewById(R.id.button_next_track);
        nextButton.setOnClickListener(v -> MPDCommandHandler.nextSong());

        final ImageButton stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(view -> MPDCommandHandler.stop());

        mPlayPauseButton = findViewById(R.id.button_playpause);
        mPlayPauseButton.setOnClickListener(view -> MPDCommandHandler.togglePause());

        if (null == mStateListener) {
            mStateListener = new ServerStatusListener();
        }

        mSwitcher.setOnClickListener(v -> {
            cancelSwitching();
            startSwitching();
            updateFanartViews();
        });

        // seekbar (position)
        mPositionSeekbar = findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(new PositionSeekbarListener());

        mVolumeSeekbar = findViewById(R.id.volume_seekbar);
        mVolumeIcon = findViewById(R.id.volume_icon);
        mVolumeIcon.setOnClickListener(view -> MPDCommandHandler.setVolume(0));
        mVolumeSeekbar.setMax(100);
        mVolumeSeekbar.setOnSeekBarChangeListener(new VolumeSeekBarListener());

        /* Volume control buttons */
        mVolumeIconButtons = findViewById(R.id.volume_icon_buttons);
        mVolumeIconButtons.setOnClickListener(view -> MPDCommandHandler.setVolume(0));

        mVolumeText = findViewById(R.id.volume_button_text);

        /* Create two listeners that start a repeating timer task to repeat the volume plus/minus action */
        mPlusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_UP, mVolumeStepSize);
        mMinusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_DOWN, mVolumeStepSize);

        final ImageButton volumeMinus = findViewById(R.id.volume_button_minus);
        volumeMinus.setOnClickListener(v -> MPDCommandHandler.decreaseVolume(mVolumeStepSize));
        volumeMinus.setOnLongClickListener(mMinusListener);
        volumeMinus.setOnTouchListener(mPlusListener);

        final ImageButton volumePlus = findViewById(R.id.volume_button_plus);
        volumePlus.setOnClickListener(v -> MPDCommandHandler.increaseVolume(mVolumeStepSize));
        volumePlus.setOnLongClickListener(mPlusListener);
        volumePlus.setOnTouchListener(mPlusListener);

        mVolumeSeekbarLayout = findViewById(R.id.volume_seekbar_layout);
        mVolumeButtonLayout = findViewById(R.id.volume_button_layout);

        mFanartManager = FanartManager.getInstance(getApplicationContext());
    }



    @Override
    protected void onResume() {
        super.onResume();


        MPDStateMonitoringHandler.getHandler().unregisterStatusListener(mStateListener);
        cancelSwitching();
        startSwitching();

        mTrackTitle.setSelected(true);
        mTrackArtist.setSelected(true);
        mTrackAlbum.setSelected(true);

        hideSystemUI();

        setVolumeControlSetting();
        }

    @Override
    protected void onPause() {
        super.onPause();

        MDPSstateMonitoringHandler.getHandler().unregisterStatusListener(mStateListerner);
        cancelSwitching();
    }

    @Override
    protected void onConnected() {
        updateMPDStatus(MPDStateMonitoringHandler.getHandler().getLastStatus());
    }

    @Override
    protected void onDisconnected(){
        udapteMPDStatus(new MPDCurrentStatus());
        udapteMPDCurrentTrack(new MPDTrack(""));
    }

    @Override
    protected void onMPDError(MPDException.MPDServerException e) {

    }

    @Override
    protected void onMPDConnectionError(MPDException.MPDConnectionException e) {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_ARTWORK_POINTER, mCurrentFanart);
        savedInstanceState.putInt(STATE_ARTWORK_POINTER_NEXT, mNextFanart);
        savedInstanceState.putParcelable(STATE_LAST_TRACK, mLastTrack);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        mCurrentFanart = savedInstanceState.getInt(STATE_ARTWORK_POINTER);
        mNextFanart = savedInstanceState.getInt(STATE_ARTWORK_POINTER_NEXT);
        mLastTrack = savedInstanceState.getParcelable(STATE_LAST_TRACK);

        restoreFanartView();
    }

    @Override
    public void fanartInitialCacheCount(final int count) {
        if (count > 0) {
            mNextFanart = 0;
            updateFanartViews();
        }
    }

    @Override
    public void fanartCacheCountChanged(final int count) {
        if (count == 1) {
            updateFanartViews();
        }

        if (mCurrentFanart == (count - 2)) {
            mNextFanart = (mCurrentFanart + 1) % count;
}





        private void updateMPDStatus(MPDCurrentStatus status) {
            MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();

            // update play buttons
            switch (state) {
                case MPD_PLAYING:
                    mPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_48dp);
                    break;
                case MPD_PAUSING:
                case MPD_STOPPED:
                    mPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_48dp);
                    break;
            }

            // Update volume seekbar
            int volume = status.getVolume();
            mVolumeSeekbar.setProgress(volume);

            if (volume >= 70) {
                mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
                mVolumeIconButtons.setImageResource(R.drawable.ic_volume_high_black_48dp);
            } else if (volume >= 30) {
                mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
                mVolumeIconButtons.setImageResource(R.drawable.ic_volume_medium_black_48dp);
            } else if (volume > 0) {
                mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
                mVolumeIconButtons.setImageResource(R.drawable.ic_volume_low_black_48dp);
            } else {
                mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
                mVolumeIconButtons.setImageResource(R.drawable.ic_volume_mute_black_48dp);
            }
            mVolumeIcon.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent)));
            mVolumeIconButtons.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent)));

            mVolumeText.setText(String.valueOf(volume) + '%');

            // Update position seekbar & textviews
            mPositionSeekbar.setMax(status.getTrackLength());
            mPositionSeekbar.setProgress(status.getElapsedTime());
        }