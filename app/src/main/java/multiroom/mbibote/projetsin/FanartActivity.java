package multiroom.mbibote.projetsin;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class FanartActivity extends AppCompatActivity implements FanartManager.OnFanartCacheChangeListener{
    private static final String TAG = FanartActivity.class.getSimpleName();

    private static final String STATE_ARTWORK_POINTER = "artwork_pointer";
    private static final String STATE_ARTWORK_POINTER_NEXT = "artwork_pointer_next";
    private static final String STATE_LAST_TRACK = "last_track";


    private static final int FANART_SWITCH_TIME =12 * 1000;

    private TextView mTracktitle;
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
    }

    @Override
    protected

    }


