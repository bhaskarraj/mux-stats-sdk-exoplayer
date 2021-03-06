package com.mux.stats.sdk.muxstats;

import android.content.Context;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.mux.stats.sdk.core.events.playback.SeekedEvent;
import com.mux.stats.sdk.core.events.playback.SeekingEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;

import java.io.IOException;

public class MuxStatsExoPlayer extends MuxBaseExoPlayer implements AnalyticsListener, Player.EventListener{

    public MuxStatsExoPlayer(Context ctx, ExoPlayer player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData, null, true);
    }

    public MuxStatsExoPlayer(Context ctx, ExoPlayer player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             CustomerViewData customerViewData) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData, customerViewData, true);
    }

    public MuxStatsExoPlayer(Context ctx, ExoPlayer player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             boolean sentryEnabled) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData, null, sentryEnabled);
    }

    public MuxStatsExoPlayer(Context ctx, ExoPlayer player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             CustomerViewData customerViewData, boolean sentryEnabled) {
        super(ctx, player, playerName, customerPlayerData, customerVideoData, customerViewData, sentryEnabled);

        if (player instanceof SimpleExoPlayer) {
            ((SimpleExoPlayer) player).addAnalyticsListener(this);
        } else {
            player.addListener(this);
        }
        if (player.getPlaybackState() == Player.STATE_BUFFERING) {
            // playback started before muxStats was initialized
            play();
            buffering();
        } else if (player.getPlaybackState() == Player.STATE_READY) {
            // We have to simulate all the events we expect to see here, even though not ideal
            play();
            buffering();
            playing();
        }
    }

    @Override
    public void release() {
        if (this.player.get() != null) {
            ExoPlayer player = this.player.get();
            if (player instanceof SimpleExoPlayer) {
                ((SimpleExoPlayer) player).removeAnalyticsListener(this);
            } else {
                player.removeListener(this);
            }
        }
        super.release();
    }

    @Override
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady,
            int playbackState) {
        onPlayerStateChanged(playWhenReady, playbackState);
    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {
        onTimelineChanged(eventTime.timeline, null, reason);
    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        onPositionDiscontinuity(reason);
    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        dispatch(new SeekingEvent(null));
    }

    @Override
    public void onSeekProcessed(EventTime eventTime) {
        onSeekProcessed();
    }

    @Override
    public void onPlaybackParametersChanged(EventTime eventTime,
            PlaybackParameters playbackParameters) {
        onPlaybackParametersChanged(playbackParameters);
    }

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        onRepeatModeChanged(repeatMode);
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime,
            boolean shuffleModeEnabled) {
        onShuffleModeEnabledChanged(shuffleModeEnabled);
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        onLoadingChanged(isLoading);
    }

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
        onPlayerError(error);
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups,
            TrackSelectionArray trackSelections) {
        onTracksChanged(trackGroups, trackSelections);
    }

    @Override
    public void onLoadStarted(EventTime eventTime,
            MediaSourceEventListener.LoadEventInfo loadEventInfo,
            MediaSourceEventListener.MediaLoadData mediaLoadData) {
        bandwidthDispatcher.onLoadStarted(loadEventInfo.dataSpec, mediaLoadData.dataType,
                mediaLoadData.trackFormat, mediaLoadData.mediaStartTimeMs,
                mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs);
    }

    @Override
    public void onLoadCompleted(EventTime eventTime,
            MediaSourceEventListener.LoadEventInfo loadEventInfo,
            MediaSourceEventListener.MediaLoadData mediaLoadData) {
        bandwidthDispatcher.onLoadCompleted(loadEventInfo.dataSpec, mediaLoadData.dataType,
                mediaLoadData.trackFormat, mediaLoadData.mediaStartTimeMs,
                mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs,
                loadEventInfo.loadDurationMs, loadEventInfo.bytesLoaded,
                loadEventInfo.responseHeaders);
    }

    @Override
    public void onLoadCanceled(EventTime eventTime,
            MediaSourceEventListener.LoadEventInfo loadEventInfo,
            MediaSourceEventListener.MediaLoadData mediaLoadData) {
        bandwidthDispatcher.onLoadCanceled(loadEventInfo.dataSpec);
    }

    @Override
    public void onLoadError(EventTime eventTime,
            MediaSourceEventListener.LoadEventInfo loadEventInfo,
            MediaSourceEventListener.MediaLoadData mediaLoadData, IOException e,
            boolean wasCanceled) {
        bandwidthDispatcher.onLoadError(loadEventInfo.dataSpec, mediaLoadData.dataType, e);
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime,
            MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (mediaLoadData.trackFormat != null && mediaLoadData.trackFormat.containerMimeType != null) {
            mimeType = mediaLoadData.trackFormat.containerMimeType;
        }
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime,
            MediaSourceEventListener.MediaLoadData mediaLoadData) {

    }

    @Override
    public void onMediaPeriodCreated(EventTime eventTime) {

    }

    @Override
    public void onMediaPeriodReleased(EventTime eventTime) {

    }

    @Override
    public void onReadingStarted(EventTime eventTime) {

    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs,
            long totalBytesLoaded, long bitrateEstimate) {

    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {

    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType,
            DecoderCounters decoderCounters) {

    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType,
            String decoderName, long initializationDurationMs) {

    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType,
            Format format) {
        if (trackType == C.TRACK_TYPE_VIDEO || trackType == C.TRACK_TYPE_DEFAULT) {
            handleRenditionChange(format);
        }
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType,
            DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {

    }

    @Override
    public void onAudioUnderrun(EventTime eventTime, int bufferSize,
            long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames,
            long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height,
            int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        sourceWidth = width;
        sourceHeight = height;
    }

    @Override
    public void onRenderedFirstFrame(EventTime eventTime, Surface surface) {

    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {

    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception e) {
        internalError(new MuxErrorException(ERROR_DRM, "DrmSessionManagerError - " + e.getMessage()));
    }

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {

    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        if (timeline != null && timeline.getWindowCount() > 0) {
            Timeline.Window window = new Timeline.Window();
            timeline.getWindow(0, window);
            sourceDuration = window.getDurationMs();
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        bandwidthDispatcher.onTracksChanged(trackGroups);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        this.playWhenReady = playWhenReady;
        PlayerState state = this.getState();
        if (state == PlayerState.PLAYING_ADS) {
            // Ignore all normal events while playing ads !!!
            return;
        }
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                // We have entered buffering
                buffering();
                // If we are expected to playWhenReady, signal the play event
                if (playWhenReady) {
                    play();
                } else if (state != PlayerState.PAUSED) {
                    pause();
                }
                break;
            case Player.STATE_ENDED:
                ended();
                break;
            case Player.STATE_READY:
                // By the time we get here, it depends on playWhenReady to know if we're playing
                if (playWhenReady) {
                    playing();
                } else if (state != PlayerState.PAUSED) {
                    pause();
                }
                break;
            case Player.STATE_IDLE:
            default:
                // don't care
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                MediaCodecRenderer.DecoderInitializationException die = (MediaCodecRenderer.DecoderInitializationException) cause;
                if (die.decoderName == null) {
                    if (die.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        internalError(new MuxErrorException(e.type, "Unable to query device decoders"));
                    } else if (die.secureDecoderRequired) {
                        internalError(new MuxErrorException(e.type, "No secure decoder for " + die.mimeType));
                    } else {
                        internalError(new MuxErrorException(e.type, "No decoder for " + die.mimeType));
                    }
                } else {
                    internalError(new MuxErrorException(e.type, "Unable to instantiate decoder for " + die.mimeType));
                }
            }
        } else if (e.type == ExoPlaybackException.TYPE_SOURCE) {
            Exception error = e.getSourceException();
            internalError(new MuxErrorException(e.type, error.getClass().getCanonicalName() + " - " + error.getMessage()));
        } else if (e.type == ExoPlaybackException.TYPE_UNEXPECTED) {
            Exception error = e.getUnexpectedException();
            internalError(new MuxErrorException(e.type, error.getClass().getCanonicalName() + " - " + error.getMessage()));
        } else {
            internalError(e);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {
        dispatch(new SeekedEvent(null));
    }
}
