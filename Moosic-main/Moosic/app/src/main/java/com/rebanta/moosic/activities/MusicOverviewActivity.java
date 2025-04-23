package com.rebanta.moosic.activities;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.media3.common.Player;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.rebanta.moosic.ApplicationClass;
import com.rebanta.moosic.R;
import com.rebanta.moosic.databinding.ActivityMusicOverviewBinding;
import com.rebanta.moosic.databinding.MusicOverviewMoreInfoBottomSheetBinding;
import com.rebanta.moosic.model.AlbumItem;
import com.rebanta.moosic.model.BasicDataRecord;
import com.rebanta.moosic.network.ApiManager;
import com.rebanta.moosic.network.utility.RequestNetwork;
import com.rebanta.moosic.records.SongResponse;
import com.rebanta.moosic.records.sharedpref.SavedLibraries;
import com.rebanta.moosic.services.ActionPlaying;
import com.rebanta.moosic.services.MusicService;
import com.rebanta.moosic.utils.SharedPreferenceManager;
import com.rebanta.moosic.utils.TrackCacheHelper;
import com.rebanta.moosic.utils.customview.BottomSheetItemView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicOverviewActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    private final String TAG = "MusicOverviewActivity";
    private final Handler handler = new Handler();
    ActivityMusicOverviewBinding binding;
    private String SONG_URL = "";
    private String ID_FROM_EXTRA = "";
    private String IMAGE_URL = "";
    MusicService musicService;
    private List<SongResponse.Artist> artsitsList = new ArrayList<>();
    private String SHARE_URL = "";
    private SongResponse mSongResponse;

    // GestureDetector for intuitive gestures
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector; // For pinch gestures

    // Audio manager for volume control
    private AudioManager audioManager;
    private int maxVolume;

    // Right edge threshold for volume control
    private final int RIGHT_EDGE_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMusicOverviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize AudioManager for volume control
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        binding.title.setSelected(true);
        binding.description.setSelected(true);

        if (!(((ApplicationClass) getApplicationContext()).getTrackQueue().size() > 1))
            binding.shuffleIcon.setVisibility(View.INVISIBLE);

        binding.playPauseImage.setOnClickListener(view -> {
            if (ApplicationClass.player.isPlaying()) {
                handler.removeCallbacks(runnable);
                ApplicationClass.player.pause();
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
            } else {
                ApplicationClass.player.play();
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
                updateSeekbar();
            }
            showNotification(ApplicationClass.player.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
        });

        binding.seekbar.setMax(100);
        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = ApplicationClass.player.getDuration();
                if (duration > 0) {
                    int playPosition = (int) ((duration / 100) * binding.seekbar.getProgress());
                    ApplicationClass.player.seekTo(playPosition);
                    binding.elapsedDuration.setText(convertDuration(ApplicationClass.player.getCurrentPosition()));
                }
            }
        });

        final ApplicationClass applicationClass = (ApplicationClass) getApplicationContext();

        binding.nextIcon.setOnClickListener(view -> applicationClass.nextTrack());
        binding.prevIcon.setOnClickListener(view -> applicationClass.prevTrack());

        binding.repeatIcon.setOnClickListener(view -> {
            if (ApplicationClass.player.getRepeatMode() == Player.REPEAT_MODE_OFF)
                ApplicationClass.player.setRepeatMode(Player.REPEAT_MODE_ONE);
            else
                ApplicationClass.player.setRepeatMode(Player.REPEAT_MODE_OFF);

            if (ApplicationClass.player.getRepeatMode() == Player.REPEAT_MODE_OFF)
                binding.repeatIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));
            else
                binding.repeatIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));

            Toast.makeText(MusicOverviewActivity.this, "Repeat Mode Changed.", Toast.LENGTH_SHORT).show();
        });

        binding.shuffleIcon.setOnClickListener(view -> {
            ApplicationClass.player.setShuffleModeEnabled(!ApplicationClass.player.getShuffleModeEnabled());
            if (ApplicationClass.player.getShuffleModeEnabled())
                binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));
            else
                binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));
            Toast.makeText(MusicOverviewActivity.this, "Shuffle Mode Changed.", Toast.LENGTH_SHORT).show();
        });

        binding.shareIcon.setOnClickListener(view -> {
            if (SHARE_URL.isBlank()) return;
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, SHARE_URL);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        });

        binding.moreIcon.setOnClickListener(view -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MusicOverviewActivity.this, R.style.MyBottomSheetDialogTheme);
            final MusicOverviewMoreInfoBottomSheetBinding _binding = MusicOverviewMoreInfoBottomSheetBinding.inflate(getLayoutInflater());
            _binding.albumTitle.setText(binding.title.getText().toString());
            _binding.albumSubTitle.setText(binding.description.getText().toString());
            Picasso.get().load(Uri.parse(IMAGE_URL)).into(_binding.coverImage);
            final LinearLayout linearLayout = _binding.main;

            _binding.goToAlbum.setOnClickListener(go_to_album -> {
                if (mSongResponse == null) return;
                if (mSongResponse.data().get(0).album() == null) return;
                final SongResponse.Album album = mSongResponse.data().get(0).album();
                startActivity(new Intent(MusicOverviewActivity.this, ListActivity.class)
                        .putExtra("type", "album")
                        .putExtra("id", album.id())
                        .putExtra("data", new Gson().toJson(new AlbumItem(album.name(), "", "", album.id())))
                );
            });

            _binding.addToLibrary.setOnClickListener(v -> {
                int index = -1;
                final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(MusicOverviewActivity.this);
                SavedLibraries savedLibraries = sharedPreferenceManager.getSavedLibrariesData();
                if (savedLibraries == null) savedLibraries = new SavedLibraries(new ArrayList<>());
                if (savedLibraries.lists().isEmpty()) {
                    Snackbar.make(_binding.getRoot(), "No Libraries Found", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                final List<String> userCreatedLibraries = new ArrayList<>();
                for (SavedLibraries.Library library : savedLibraries.lists()) {
                    if (library.isCreatedByUser())
                        userCreatedLibraries.add(library.name());
                }
                MaterialAlertDialogBuilder materialAlertDialogBuilder = getMaterialAlertDialogBuilder(userCreatedLibraries, savedLibraries, sharedPreferenceManager);
                materialAlertDialogBuilder.show();
            });

            _binding.download.setOnClickListener(v -> {
                final TrackCacheHelper trackCacheHelper = new TrackCacheHelper(MusicOverviewActivity.this);
                final SongResponse.Song song = mSongResponse.data().get(0);
                if (trackCacheHelper.isTrackInCache(song.id())) {
                    trackCacheHelper.copyFileToMusicDir(trackCacheHelper.getTrackFromCache(song.id()), song.name());
                    Toast.makeText(MusicOverviewActivity.this, "Downloaded to /Music/Melotune/ ", Toast.LENGTH_SHORT).show();
                } else {
                    ProgressDialog progressDialog = new ProgressDialog(MusicOverviewActivity.this);
                    progressDialog.setMessage("Downloading...");
                    progressDialog.setCancelable(false);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (ApplicationClass.isTrackDownloaded) {
                                progressDialog.dismiss();
                                trackCacheHelper.copyFileToMusicDir(trackCacheHelper.getTrackFromCache(song.id()), song.name());
                                Toast.makeText(MusicOverviewActivity.this, "Downloaded to /Music/Melotune/ ", Toast.LENGTH_SHORT).show();
                                this.cancel();
                            }
                        }
                    }, 1, 1000);
                }
            });

            for (SongResponse.Artist artist : artsitsList) {
                try {
                    final String imgUrl = artist.image().isEmpty() ? "" : artist.image().get(artist.image().size() - 1).url();
                    BottomSheetItemView bottomSheetItemView = new BottomSheetItemView(MusicOverviewActivity.this, artist.name(), imgUrl, artist.id());
                    bottomSheetItemView.setFocusable(true);
                    bottomSheetItemView.setClickable(true);
                    bottomSheetItemView.setOnClickListener(view1 -> {
                        Log.i(TAG, "BottomSheetItemView: onCLicked!");
                        startActivity(new Intent(MusicOverviewActivity.this, ArtistProfileActivity.class)
                                .putExtra("data", new Gson().toJson(
                                        new BasicDataRecord(artist.id(), artist.name(), "", imgUrl)))
                        );
                    });
                    linearLayout.addView(bottomSheetItemView);
                } catch (Exception e) {
                    Log.e(TAG, "BottomSheetDialog: ", e);
                }
            }
            bottomSheetDialog.setContentView(_binding.getRoot());
            bottomSheetDialog.create();
            bottomSheetDialog.show();
        });

        binding.trackQuality.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MusicOverviewActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.track_quality_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                Toast.makeText(MusicOverviewActivity.this, menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                ApplicationClass.setTrackQuality(menuItem.getTitle().toString());
                onSongFetched(mSongResponse, true);
                prepareMediaPLayer();
                binding.trackQuality.setText(ApplicationClass.TRACK_QUALITY);
                return true;
            });
            popupMenu.show();
        });

        binding.trackQuality.setText(ApplicationClass.TRACK_QUALITY);

        showData();
        updateTrackInfo();

        // --- GestureDetector Setup for Intuitive Gestures ---
        gestureDetector = new GestureDetector(this, new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new PinchListener());

        // Attach to album art for all gestures
        binding.coverImage.setOnTouchListener((v, event) -> {
            // Handle two-finger swipe for seek
            if (event.getPointerCount() == 2) {
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    twoFingerStartX = event.getX(0);
                    twoFingerStartY = event.getY(0);
                    twoFingerStartX2 = event.getX(1);
                    twoFingerStartY2 = event.getY(1);
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float dx1 = event.getX(0) - twoFingerStartX;
                    float dx2 = event.getX(1) - twoFingerStartX2;
                    // If both fingers move horizontally in the same direction
                    if (Math.abs(dx1) > 60 && Math.abs(dx2) > 60 && Math.signum(dx1) == Math.signum(dx2)) {
                        if (dx1 > 0 && dx2 > 0) {
                            seekBy(-10000); // Seek backward 10s
                        } else if (dx1 < 0 && dx2 < 0) {
                            seekBy(10000); // Seek forward 10s
                        }
                        // Reset start positions to avoid repeated seeking
                        twoFingerStartX = event.getX(0);
                        twoFingerStartX2 = event.getX(1);
                    }
                }
                return true;
            }
            scaleGestureDetector.onTouchEvent(event); // Pinch in/out
            return gestureDetector.onTouchEvent(event); // Single/double tap, swipe, etc.
        });

        // Attach to root view for volume control and other actions
        binding.getRoot().setOnTouchListener((v, event) -> {
            // Handle volume control for right edge swipes
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastX = event.getX();
                lastY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (lastX > getWindowManager().getDefaultDisplay().getWidth() - RIGHT_EDGE_THRESHOLD) {
                    float deltaY = lastY - event.getY();
                    if (Math.abs(deltaY) > 10) {
                        adjustVolumeByDelta(deltaY);
                        lastY = event.getY();
                        return true;
                    }
                }
            }
            return gestureDetector.onTouchEvent(event);
        });
    }

    // Track touch positions for volume control and two-finger swipe
    private float lastX = 0, lastY = 0;
    private float twoFingerStartX = 0, twoFingerStartY = 0, twoFingerStartX2 = 0, twoFingerStartY2 = 0;

    // Adjust volume based on vertical swipe
    private void adjustVolumeByDelta(float deltaY) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (deltaY > 0) {
            if (currentVolume < maxVolume) {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                );
            }
        } else {
            if (currentVolume > 0) {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                );
            }
        }
    }

    // --- GestureListener for tap, double tap, swipe, long press ---
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            binding.playPauseImage.performClick();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            likeCurrentTrack();
            showHeartOverlay();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            binding.moreIcon.performClick();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Only handle left/right/up/down swipes for track control if not on right edge
            if (e1.getX() < getWindowManager().getDefaultDisplay().getWidth() - RIGHT_EDGE_THRESHOLD) {
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                        if (diffX > 0) {
                            ((ApplicationClass) getApplicationContext()).prevTrack();
                            return true;
                        } else {
                            ((ApplicationClass) getApplicationContext()).nextTrack();
                            return true;
                        }
                    }
                } else if (diffY > SWIPE_THRESHOLD) {
                    // Swipe Down: Dismiss Activity
                    finish();
                    return true;
                } else if (diffY < -SWIPE_THRESHOLD) {
                    // Swipe Up: Show queue or lyrics
                    showQueueOrLyrics();
                    return true;
                }
            }
            return false;
        }
    }

    // --- Pinch gesture listener for album art/lyrics zoom ---
    private class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (detector.getScaleFactor() > 1) {
                showLyricsOrExpandArt();
            } else {
                minimizeArtOrHideLyrics();
            }
            return true;
        }
    }

    // --- Placeholder methods for new gestures ---
    private void likeCurrentTrack() {
        // TODO: Implement "like" logic
        Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
    }
    private void showHeartOverlay() {
        // TODO: Show heart animation overlay
    }
    private void seekBy(long millis) {
        long pos = ApplicationClass.player.getCurrentPosition();
        long duration = ApplicationClass.player.getDuration();
        long newPos = Math.max(0, Math.min(pos + millis, duration));
        ApplicationClass.player.seekTo(newPos);
        binding.elapsedDuration.setText(convertDuration(newPos));
    }
    private void showLyricsOrExpandArt() {
        // TODO: Show lyrics or expand album art
        Toast.makeText(this, "Expand/Show Lyrics", Toast.LENGTH_SHORT).show();
    }
    private void minimizeArtOrHideLyrics() {
        // TODO: Minimize album art or hide lyrics
        Toast.makeText(this, "Minimize/Hide Lyrics", Toast.LENGTH_SHORT).show();
    }
    private void showQueueOrLyrics() {
        // TODO: Show queue or lyrics
        Toast.makeText(this, "Show Queue/Lyrics", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private MaterialAlertDialogBuilder getMaterialAlertDialogBuilder(List<String> userCreatedLibraries, SavedLibraries savedLibraries, SharedPreferenceManager sharedPreferenceManager) {
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MusicOverviewActivity.this);
        ListAdapter listAdapter = new ArrayAdapter<>(MusicOverviewActivity.this, android.R.layout.simple_list_item_1, userCreatedLibraries);
        final SavedLibraries finalSavedLibraries = savedLibraries;
        materialAlertDialogBuilder.setAdapter(listAdapter, (dialogInterface, i) -> {
            final SongResponse.Song song = mSongResponse.data().get(0);
            SavedLibraries.Library.Songs songs = new SavedLibraries.Library.Songs(
                    song.id(),
                    song.name(),
                    binding.description.getText().toString(),
                    IMAGE_URL
            );
            finalSavedLibraries.lists().get(i).songs().add(songs);
            sharedPreferenceManager.setSavedLibrariesData(finalSavedLibraries);
            Toast.makeText(MusicOverviewActivity.this, "Added to " + finalSavedLibraries.lists().get(i).name(), Toast.LENGTH_SHORT).show();
        });
        materialAlertDialogBuilder.setTitle("Select Library");
        return materialAlertDialogBuilder;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        musicService.setCallback(MusicOverviewActivity.this);
        Log.e(TAG, "onServiceConnected: ");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onServiceDisconnected: ");
        musicService = null;
    }

    void showData() {
        if (getIntent().getExtras() == null) return;
        final ApiManager apiManager = new ApiManager(this);
        final String ID = getIntent().getExtras().getString("id", "");
        ID_FROM_EXTRA = ID;
        if (ApplicationClass.MUSIC_ID.equals(ID)) {
            updateSeekbar();
            if (ApplicationClass.player.isPlaying())
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
            else
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
        }
        if (getIntent().getExtras().getString("type", "").equals("clear")) {
            ApplicationClass applicationClass = (ApplicationClass) getApplicationContext();
            applicationClass.setTrackQueue(new ArrayList<>(Collections.singletonList(ID)));
        }
        if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID))
            onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
        else
            apiManager.retrieveSongById(ID, null, new RequestNetwork.RequestListener() {
                @Override
                public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                    SongResponse songResponse = new Gson().fromJson(response, SongResponse.class);
                    if (songResponse.success()) {
                        onSongFetched(songResponse);
                        SharedPreferenceManager.getInstance(MusicOverviewActivity.this).setSongResponseById(ID, songResponse);
                    } else if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID))
                        onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
                    else
                        finish();
                }
                @Override
                public void onErrorResponse(String tag, String message) {
                    if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID))
                        onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
                    else
                        Toast.makeText(MusicOverviewActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void onSongFetched(SongResponse songResponse) {
        onSongFetched(songResponse, false);
    }

    private void onSongFetched(SongResponse songResponse, boolean forced) {
        mSongResponse = songResponse;
        ApplicationClass.CURRENT_TRACK = mSongResponse;
        binding.title.setText(songResponse.data().get(0).name());
        binding.description.setText(
                String.format("%s plays | %s | %s",
                        convertPlayCount(songResponse.data().get(0).playCount()),
                        songResponse.data().get(0).year(),
                        songResponse.data().get(0).copyright())
        );
        List<SongResponse.Image> image = songResponse.data().get(0).image();
        IMAGE_URL = image.get(image.size() - 1).url();
        SHARE_URL = songResponse.data().get(0).url();
        Picasso.get().load(Uri.parse(image.get(image.size() - 1).url())).into(binding.coverImage);
        List<SongResponse.DownloadUrl> downloadUrls = songResponse.data().get(0).downloadUrl();
        artsitsList = songResponse.data().get(0).artists().primary();
        SONG_URL = ApplicationClass.getDownloadUrl(downloadUrls);
        if ((!ApplicationClass.MUSIC_ID.equals(ID_FROM_EXTRA) || forced)) {
            ApplicationClass applicationClass = (ApplicationClass) getApplicationContext();
            applicationClass.setMusicDetails(IMAGE_URL, binding.title.getText().toString(), binding.description.getText().toString(), ID_FROM_EXTRA);
            applicationClass.setSongUrl(SONG_URL);
            prepareMediaPLayer();
        }
    }

    public void backPress(View view) {
        finish();
    }

    public static String convertPlayCount(int playCount) {
        if (playCount < 1000) return playCount + "";
        if (playCount < 1000000) return playCount / 1000 + "K";
        return playCount / 1000000 + "M";
    }

    public static String convertDuration(long duration) {
        if (duration <= 0 || duration > 24 * 60 * 60 * 1000) {
            return "0:00";
        }
        String timeString = "";
        String secondString;
        int hours = (int) (duration / (1000 * 60 * 60));
        int minutes = (int) ((duration % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) ((duration % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hours > 0) {
            timeString = hours + ":";
        }
        if (seconds < 10) {
            secondString = "0" + seconds;
        } else {
            secondString = "" + seconds;
        }
        timeString = timeString + minutes + ":" + secondString;
        return timeString;
    }

    void prepareMediaPLayer() {
        try {
            ((ApplicationClass) getApplicationContext()).prepareMediaPlayer();
            long duration = ApplicationClass.player.getDuration();
            if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                binding.totalDuration.setText(convertDuration(duration));
            } else {
                binding.totalDuration.setText("0:00");
            }
            playClicked();
            binding.playPauseImage.performClick();
        } catch (Exception e) {
            Log.e(TAG, "Error preparing media player: " + e.getMessage());
            Toast.makeText(this, "Error playing track", Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable runnable = this::updateSeekbar;

    void updateSeekbar() {
        if (ApplicationClass.player.isPlaying()) {
            long duration = ApplicationClass.player.getDuration();
            long currentPosition = ApplicationClass.player.getCurrentPosition();
            if (duration > 0 && currentPosition >= 0) {
                binding.seekbar.setProgress((int) (((float) currentPosition / duration) * 100));
                binding.elapsedDuration.setText(convertDuration(currentPosition));
            } else {
                binding.seekbar.setProgress(0);
                binding.elapsedDuration.setText("0:00");
            }
            handler.postDelayed(runnable, 1000);
        }
    }

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateTimeTask = this::updateTrackInfo;

    private void updateTrackInfo() {
        if (!binding.title.getText().toString().equals(ApplicationClass.MUSIC_TITLE))
            binding.title.setText(ApplicationClass.MUSIC_TITLE);
        if (!binding.description.getText().toString().equals(ApplicationClass.MUSIC_DESCRIPTION))
            binding.description.setText(ApplicationClass.MUSIC_DESCRIPTION);
        if (ApplicationClass.IMAGE_URL != null && !ApplicationClass.IMAGE_URL.isEmpty())
            Picasso.get().load(Uri.parse(ApplicationClass.IMAGE_URL)).into(binding.coverImage);

        long duration = ApplicationClass.player.getDuration();
        long currentPosition = ApplicationClass.player.getCurrentPosition();

        if (duration > 0 && currentPosition >= 0) {
            binding.seekbar.setProgress((int) (((float) currentPosition / duration) * 100));
            binding.seekbar.setSecondaryProgress((int) (((float) ApplicationClass.player.getBufferedPosition() / duration) * 100));
            binding.elapsedDuration.setText(convertDuration(currentPosition));
            binding.totalDuration.setText(convertDuration(duration));
        }

        if (ApplicationClass.player.isPlaying())
            binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
        else
            binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);

        if (ApplicationClass.player.getRepeatMode() == Player.REPEAT_MODE_OFF)
            binding.repeatIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));
        else
            binding.repeatIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));

        if (ApplicationClass.player.getShuffleModeEnabled())
            binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));
        else
            binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));

        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }

    @Override
    public void nextClicked() { }
    @Override
    public void prevClicked() { }
    @Override
    public void playClicked() {
        if (!ApplicationClass.player.isPlaying()) {
            binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
        } else {
            binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
        }
    }
    @Override
    public void onProgressChanged(int progress) { }

    public void showNotification(int playPauseButton) {
        ApplicationClass applicationClass = (ApplicationClass) getApplicationContext();
        applicationClass.setMusicDetails(IMAGE_URL, binding.title.getText().toString(), binding.description.getText().toString(), getIntent().getExtras().getString("id", ""));
        applicationClass.showNotification();
    }
}
