package com.rebanta.moosic.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.rebanta.moosic.records.AlbumSearch;
import com.rebanta.moosic.records.AlbumsSearch;
import com.rebanta.moosic.records.ArtistSearch;
import com.rebanta.moosic.records.ArtistsSearch;
import com.rebanta.moosic.records.GlobalSearch;
import com.rebanta.moosic.records.PlaylistSearch;
import com.rebanta.moosic.records.PlaylistsSearch;
import com.rebanta.moosic.records.SongResponse;
import com.rebanta.moosic.records.SongSearch;
import com.rebanta.moosic.records.sharedpref.SavedLibraries;

import java.util.ArrayList;

public class SharedPreferenceManager {

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    private final SharedPreferences sharedPreferences;

    private static SharedPreferenceManager instance;

    public static SharedPreferenceManager getInstance(Context context) {
        if (instance == null) instance = new SharedPreferenceManager(context);
        return instance;
    }

    private SharedPreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences("cache", Context.MODE_PRIVATE);
    }

    public void setHomeSongsRecommended(SongSearch songSearch) {
        sharedPreferences.edit().putString("home_songs_recommended", new Gson().toJson(songSearch)).apply();
    }

    public SongSearch getHomeSongsRecommended() {
        return new Gson().fromJson(sharedPreferences.getString("home_songs_recommended", ""), SongSearch.class);
    }

    public void setHomeArtistsRecommended(ArtistsSearch artistsRecommended) {
        sharedPreferences.edit().putString("home_artists_recommended", new Gson().toJson(artistsRecommended)).apply();
    }

    public ArtistsSearch getHomeArtistsRecommended() {
        return new Gson().fromJson(sharedPreferences.getString("home_artists_recommended", ""), ArtistsSearch.class);
    }

    public void setHomeAlbumsRecommended(AlbumsSearch albumsSearch) {
        sharedPreferences.edit().putString("home_albums_recommended", new Gson().toJson(albumsSearch)).apply();
    }

    public AlbumsSearch getHomeAlbumsRecommended() {
        return new Gson().fromJson(sharedPreferences.getString("home_albums_recommended", ""), AlbumsSearch.class);
    }

    public void setHomePlaylistRecommended(PlaylistsSearch playlistsSearch) {
        sharedPreferences.edit().putString("home_playlists_recommended", new Gson().toJson(playlistsSearch)).apply();
    }

    public PlaylistsSearch getHomePlaylistRecommended() {
        return new Gson().fromJson(sharedPreferences.getString("home_playlists_recommended", ""), PlaylistsSearch.class);
    }

    public void setSongResponseById(String id, SongResponse songSearch) {
        sharedPreferences.edit().putString(id, new Gson().toJson(songSearch)).apply();
    }

    public SongResponse getSongResponseById(String id) {
        return new Gson().fromJson(sharedPreferences.getString(id, ""), SongResponse.class);
    }

    public boolean isSongResponseById(String id) {
        return sharedPreferences.contains(id);
    }

    public void setAlbumResponseById(String id, AlbumSearch albumSearch) {
        sharedPreferences.edit().putString(id, new Gson().toJson(albumSearch)).apply();
    }

    public AlbumSearch getAlbumResponseById(String id) {
        return new Gson().fromJson(sharedPreferences.getString(id, ""), AlbumSearch.class);
    }

    public void setPlaylistResponseById(String id, PlaylistSearch playlistSearch) {
        sharedPreferences.edit().putString(id, new Gson().toJson(playlistSearch)).apply();
    }

    public PlaylistSearch getPlaylistResponseById(String id) {
        return new Gson().fromJson(sharedPreferences.getString(id, ""), PlaylistSearch.class);
    }

    public void setTrackQuality(String string) {
        sharedPreferences.edit().putString("track_quality", string).apply();
    }

    public String getTrackQuality() {
        return sharedPreferences.getString("track_quality", "320kbps");
    }

    public void setSavedLibrariesData(SavedLibraries savedLibraries) {
        sharedPreferences.edit().putString("saved_libraries", new Gson().toJson(savedLibraries)).apply();
    }

    public SavedLibraries getSavedLibrariesData() {
        return new Gson().fromJson(sharedPreferences.getString("saved_libraries", ""), SavedLibraries.class);
    }

    public void addLibraryToSavedLibraries(SavedLibraries.Library library) {
        SavedLibraries savedLibraries = getSavedLibrariesData();
        if (savedLibraries == null) savedLibraries = new SavedLibraries(new ArrayList<>());
        savedLibraries.lists().add(library);
        setSavedLibrariesData(savedLibraries);
    }

    public void removeLibraryFromSavedLibraries(int index) {
        SavedLibraries savedLibraries = getSavedLibrariesData();
        if (savedLibraries == null) return;
        savedLibraries.lists().remove(index);
        setSavedLibrariesData(savedLibraries);
    }

    public void setSavedLibraryDataById(String id, SavedLibraries.Library library) {
        sharedPreferences.edit().putString(id, new Gson().toJson(library)).apply();
    }

    public SavedLibraries.Library getSavedLibraryDataById(String id) {
        return sharedPreferences.contains(id) ? new Gson().fromJson(sharedPreferences.getString(id, ""), SavedLibraries.Library.class) : null;
    }

    public void setSearchResultCache(String query, GlobalSearch searchResult) {
        sharedPreferences.edit().putString("search://" + query, new Gson().toJson(searchResult)).apply();
    }

    public GlobalSearch getSearchResult(String query) {
        return sharedPreferences.contains("search://" + query) ? new Gson().fromJson(sharedPreferences.getString("search://" + query, ""), GlobalSearch.class) : null;
    }

    public void setArtistData(String artistID, ArtistSearch artistSearch) {
        sharedPreferences.edit().putString("artistData://" + artistID, new Gson().toJson(artistSearch)).apply();
    }

    public ArtistSearch getArtistData(String artistId) {
        return sharedPreferences.contains("artistData://" + artistId) ? new Gson().fromJson(sharedPreferences.getString("artistData://" + artistId, ""), ArtistSearch.class) : null;
    }
}
