package com.rebanta.moosic.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.rebanta.moosic.adapters.ActivityArtistProfileTopAlbumsAdapter;
import com.rebanta.moosic.adapters.ActivityArtistProfileTopSongsAdapter;
import com.rebanta.moosic.adapters.ActivitySeeMoreListAdapter;
import com.rebanta.moosic.databinding.ActivityArtistProfileBinding;
import com.rebanta.moosic.model.BasicDataRecord;
import com.rebanta.moosic.network.ApiManager;
import com.rebanta.moosic.network.NetworkChangeReceiver;
import com.rebanta.moosic.network.utility.RequestNetwork;
import com.rebanta.moosic.records.AlbumsSearch;
import com.rebanta.moosic.records.ArtistSearch;
import com.rebanta.moosic.records.SongResponse;
import com.rebanta.moosic.utils.SharedPreferenceManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtistProfileActivity extends AppCompatActivity {

    private final String TAG = "ArtistProfileActivity";
    ActivityArtistProfileBinding binding;

    NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkStatusListener() {
        @Override
        public void onNetworkConnected() {
            showData();
        }

        @Override
        public void onNetworkDisconnected() {
            Snackbar.make(binding.getRoot(), "No Internet Connection", Snackbar.LENGTH_LONG).show();
            //showData();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtistProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.collapsingToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        binding.collapsingToolbarLayout.setTitle("Artist Name");

        binding.collapsingToolbarAppbarlayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (verticalOffset == 0) {
            } else {
            }
        });

        binding.topSongsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.topAlbumsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.topSinglesRecyclerview.setLayoutManager(new LinearLayoutManager(this));

        binding.topSongsSeeMore.setOnClickListener(v -> {
            startActivity(new Intent(ArtistProfileActivity.this, SeeMoreActivity.class)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_SONGS.name())
                    .putExtra("artist_name", binding.artistName.getText().toString()));
        });
        binding.topAlbumsSeeMore.setOnClickListener(v -> {
            startActivity(new Intent(ArtistProfileActivity.this, SeeMoreActivity.class)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_ALBUMS.name())
                    .putExtra("artist_name", binding.artistName.getText().toString()));
        });
        binding.topSinglesSeeMore.setOnClickListener(v -> {

        });

        showShimmerData();
        //showData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkChangeReceiver.registerReceiver(this, networkChangeReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkChangeReceiver.unregisterReceiver(this, networkChangeReceiver);
    }

    private String artistId = "9999";
    private ArtistSearch artistSearch;

    void showData() {
        if (getIntent().getExtras() == null) return;
        Log.i(TAG, "showData: " + getIntent().getExtras());
        final String artist = getIntent().getExtras().getString("data", "null");
        final BasicDataRecord artistItem = new Gson().fromJson(artist, BasicDataRecord.class);
        if (artistItem == null) return;
        artistId = artistItem.id();

        Picasso.get().load(Uri.parse(artistItem.image())).into(binding.artistImg);
        binding.artistName.setText(artistItem.title());
        binding.collapsingToolbarLayout.setTitle(artistItem.title());

        final ApiManager apiManager = new ApiManager(this);
        apiManager.retrieveArtistById(artistId, new RequestNetwork.RequestListener() {
            final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(ArtistProfileActivity.this);

            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                artistSearch = new Gson().fromJson(response, ArtistSearch.class);
                Log.i(TAG, "onResponse: " + response);
                sharedPreferenceManager.setArtistData(artistId, artistSearch);
                Log.i(TAG, "onResponse: " + sharedPreferenceManager.getArtistData(artistId));
                display();
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                Log.i(TAG, "onErrorResponse: " + message);
                ArtistSearch offlineData = sharedPreferenceManager.getArtistData(artistId);
                if (offlineData != null) {
                    artistSearch = offlineData;
                    display();
                }
//                if (!NetworkUtil.isNetworkAvailable(ArtistProfileActivity.this)) {
//                    try {
//                        Thread.sleep(2000);
//                        showData();
//                    } catch (InterruptedException e) {
//                        Log.e(TAG, "onErrorResponse: ", e);
//                    }
//                }
            }


        });
    }

    private void display() {
        Log.i(TAG, "display: " + artistSearch);
        if (artistSearch.success()) {
            Picasso.get().load(Uri.parse(artistSearch.data().image().get(artistSearch.data().image().size() - 1).url())).into(binding.artistImg);
            binding.artistName.setText(artistSearch.data().name());
            binding.collapsingToolbarLayout.setTitle(artistSearch.data().name());
            binding.topSongsRecyclerview.setAdapter(new ActivityArtistProfileTopSongsAdapter(artistSearch.data().topSongs()));
            binding.topAlbumsRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(artistSearch.data().topAlbums()));
            binding.topSinglesRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(artistSearch.data().singles()));
        }
    }

    @NonNull
    private static List<SongResponse.Song> getShimmerData() {
        final List<SongResponse.Song> shimmerData = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            shimmerData.add(new SongResponse.Song(
                    "<shimmer>",
                    "",
                    "",
                    "",
                    "",
                    0.0,
                    "",
                    false,
                    0,
                    "",
                    false,
                    "",
                    new SongResponse.Lyrics("", "", ""),
                    "",
                    "",
                    new SongResponse.Album("", "", ""),
                    new SongResponse.Artists(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                    new ArrayList<>(),
                    new ArrayList<>()
            ));
        }
        return shimmerData;
    }


    void showShimmerData() {
        final List<SongResponse.Song> shimmerData = getShimmerData();
        final List<AlbumsSearch.Data.Results> shimmerDataAlbum = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            shimmerDataAlbum.add(new AlbumsSearch.Data.Results(
                    "<shimmer>",
                    null,
                    null,
                    null,
                    0,
                    null,
                    0,
                    null,
                    false,
                    null,
                    null
            ));
        }

        binding.topSongsRecyclerview.setAdapter(new ActivityArtistProfileTopSongsAdapter(shimmerData));
        binding.topAlbumsRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(shimmerDataAlbum));
        binding.topSinglesRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(shimmerDataAlbum));

        ArtistSearch offlineData = SharedPreferenceManager.getInstance(this).getArtistData(artistId);
        if (offlineData != null) {
            artistSearch = offlineData;
            display();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}