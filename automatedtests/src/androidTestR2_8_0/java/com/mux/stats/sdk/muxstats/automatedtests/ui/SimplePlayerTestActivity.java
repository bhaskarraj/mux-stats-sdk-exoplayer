package com.mux.stats.sdk.muxstats.automatedtests.ui;


import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.RandomTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;

public class SimplePlayerTestActivity extends SimplePlayerBaseActivity {

    public void initExoPlayer() {
        TrackSelection.Factory trackSelectionFactory = new RandomTrackSelection.Factory();
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
    }
}
