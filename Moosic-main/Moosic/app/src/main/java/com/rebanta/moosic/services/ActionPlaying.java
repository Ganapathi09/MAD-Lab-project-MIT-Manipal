package com.rebanta.moosic.services;

public interface ActionPlaying {

    void nextClicked();

    void prevClicked();

    void playClicked();
    void onProgressChanged(int progress);

}
