package com.rebanta.moosic.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.rebanta.moosic.ApplicationClass;
import com.rebanta.moosic.utils.TrackCacheHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class TrackManager extends AsyncTask<String, String, String> {

    private static final String TAG = "TrackManager";
    private final Context context;
    private final String musicURL, trackName, trackId, trackImage;
    private final boolean toCache;

    public TrackManager(Context context, String musicURL, String trackName, String trackId, String trackImage, boolean toCache) {
        this.context = context;
        this.musicURL = musicURL;
        this.trackName = trackName;
        this.trackId = trackId;
        this.trackImage = trackImage;
        this.toCache = toCache;
    }

    @Override
    protected String doInBackground(String... strings) {
        int count;
        try {
            URL url = new URL(musicURL);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            File file;
//            if (!toCache)
//                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), trackName + ".mp3");
//            else
//                file = new File(context.getCacheDir(), trackId + ".mp3");
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), trackName + ".mp3");
            OutputStream output = new FileOutputStream(file);

            byte[] data = new byte[1024];

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            Log.i(TAG, "doInBackground: " + file.getCanonicalPath());
            Log.i(TAG, "doInBackground: " + file.getPath());
            Log.i(TAG, "doInBackground: " + file.getAbsolutePath());

            //new TrackCacheHelper(context).addAlbumArt(file.getAbsolutePath(), trackImage);

            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ", e);
        }

        return "FAILED";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.i(TAG, "onPostExecute: " + s);
        if (s.equals("FAILED")) return;
        new TrackCacheHelper(context).setTrackToCache(trackId, s);
        ApplicationClass.isTrackDownloaded = true;
    }
}
