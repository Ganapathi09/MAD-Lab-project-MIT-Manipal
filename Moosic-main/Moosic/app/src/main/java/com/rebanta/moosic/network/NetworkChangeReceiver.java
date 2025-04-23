package com.rebanta.moosic.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {

    // Callback interface to notify when the internet is connected or disconnected
    public interface NetworkStatusListener {
        void onNetworkConnected();

        void onNetworkDisconnected();
    }

    private final NetworkStatusListener listener;

    public NetworkChangeReceiver(NetworkStatusListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            listener.onNetworkConnected(); // Internet is connected
        } else {
            listener.onNetworkDisconnected(); // No internet connection
        }
    }

    // Register the receiver
    public static void registerReceiver(Context context, NetworkChangeReceiver receiver) {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
    }

    // Unregister the receiver
    public static void unregisterReceiver(Context context, NetworkChangeReceiver receiver) {
        context.unregisterReceiver(receiver);
    }
}
