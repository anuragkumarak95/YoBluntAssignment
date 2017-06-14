package com.yoblunt.anuragkumar.yobluntassingment.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.graphics.drawable.DrawableWrapper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.yoblunt.anuragkumar.yobluntassingment.PlayerControl;
import com.yoblunt.anuragkumar.yobluntassingment.R;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anurag Kumar on 14-Jun-17.
 */

public class HlsListAdapter extends RecyclerView.Adapter<HlsListAdapter.ViewHolder> {

    private final String hlsList[];
    private final Context context;





    public HlsListAdapter(Context context,String hlsList[]) {

        this.hlsList = hlsList;
        this.context = context;


    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if(holder.getExoPlayer()!=null) holder.releasePlayer();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.player_skeleton,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //providing every unit with its relevant HLS URI.
        holder.setHlsUri(hlsList[position]);
    }

    @Override
    public int getItemCount() {
        return hlsList.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        //layout components
        private TextureView textureView;
        private TextView textView;
        private ImageButton button;
        private SeekBar seekBar;

        //exo-player components
        private DataSource.Factory dataSourceFactory;
        private MediaSource videoSource;
        private Uri uri;
        private Handler mainHandler;
        private String userAgent;
        private TrackSelection.Factory videoTrackSelectionFactory;
        private TrackSelector trackSelector;
        private LoadControl loadControl;
        private SimpleExoPlayer exoPlayer;
        private PlayerControl playerControl;
        private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        private boolean init = true;
        private long realDurationMillis;
        private boolean duraset = false;
        private boolean isPlay = false;

        private String hlsUri;


        private ViewHolder(View v) {
            super(v);
            textureView = (TextureView) v.findViewById(R.id.textureView);
            textView = (TextView) v.findViewById(R.id.seekerText);
            button = (ImageButton) v.findViewById(R.id.play_pause_button);
            seekBar = (SeekBar) v.findViewById(R.id.seekbar);
            ((LinearLayout)v.findViewById(R.id.playercontrols)).getBackground().setAlpha(80);



            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //load player with data-source and play when ready.
                    if(init){
                        if(exoPlayer!=null) releasePlayer();

                        userAgent = Util.getUserAgent(context,"yobluntassignment");
                        createPlayer();
                        attachPlayerView();
                        preparePlayer();
                        playerControl = new PlayerControl(exoPlayer);

                        seekBar.setMax(1000);
                        init = false;

                        exoPlayer.addListener(new ExoPlayer.EventListener() {
                            @Override
                            public void onLoadingChanged(boolean isLoading) {

                            }

                            @Override
                            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                                if(playbackState == ExoPlayer.STATE_READY && !duraset){
                                    realDurationMillis = exoPlayer.getDuration();
                                    seekBar.setMax((int) realDurationMillis);
                                    duraset=true;
                                }
                            }

                            @Override
                            public void onTimelineChanged(Timeline timeline, Object manifest) {

                            }

                            @Override
                            public void onPlayerError(ExoPlaybackException error) {

                            }

                            @Override
                            public void onPositionDiscontinuity() {

                            }
                        });

                    }

                    play_pause_Track();
                    //volume controls
                    float vol = exoPlayer.getVolume();
                    //mute
                    //exoPlayer.setVolume(0f);

                    final Handler hd  =new Handler();
                    final Runnable checkUi = new Runnable() {
                        @Override
                        public void run() {
                            long currentDuration = playerControl.getDuration()-playerControl.getCurrentPosition();
                            long seconds = TimeUnit.MILLISECONDS.toSeconds(currentDuration);
                            long minutes = seconds/60;
                            long remaining_sec = seconds%60;
                            textView.setText(minutes+":"+remaining_sec);
                            seekBar.setProgress(playerControl.getCurrentPosition());
                            hd.postDelayed(this, 100);
                        }
                    };
                    checkUi.run();

                }
            });



            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //seek to a point in video implementation.
                    playerControl.seekTo(seekBar.getProgress());
                }
            });
        }

        public SimpleExoPlayer getExoPlayer() {
            return exoPlayer;
        }

        public TextureView getTextureView() {
            return textureView;
        }

        public TextView getTextView() {
            return textView;
        }

        public ImageButton getButton() {
            return button;
        }

        private void setHlsUri(String hlsUri) {
            this.hlsUri = hlsUri;
        }


        // Create TrackSelection Factory, Track Selector, Handler, Load Control, and ExoPlayer Instance
        private void createPlayer(){
            mainHandler = new Handler();
            videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
            trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
            loadControl = new DefaultLoadControl();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context,trackSelector,loadControl);
        }


        // Set player to SimpleExoPlayerView
        private void attachPlayerView() {
            exoPlayer.setVideoTextureView(textureView);
        }

        // Build Data Source Factory, Dash Media Source, and Prepare player using videoSource
        private void preparePlayer() {
            uriParse();
            dataSourceFactory = buildDataSourceFactory(bandwidthMeter);
            videoSource = new HlsMediaSource(uri, dataSourceFactory, 1, null, null);
            final LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);
            exoPlayer.prepare(loopingSource);

        }

        private void play_pause_Track(){
            if (playerControl.isPlaying()) {playerControl.pause(); isPlay = false;}
            else {playerControl.start(); isPlay=true;}

            toggleButtonText();

            seekBar.setProgress(playerControl.getCurrentPosition());
        }

        private void toggleButtonText(){
            if(isPlay) {
                Log.e("Icon verifier","Button pressed check.");
                button.setImageResource(R.drawable.ic_media_pause);}
            else button.setImageResource(R.drawable.ic_media_play);
        }
        // Parse VIDEO_URI and Save at uri variable
        private void uriParse() {
            uri = Uri.parse(hlsUri);
        }

        // Build Data Source Factory using DefaultBandwidthMeter and HttpDataSource.Factory
        private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
            return new DefaultDataSourceFactory(context, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
        }

        // Build Http Data Source Factory using DefaultBandwidthMeter
        private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
            return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
        }

        //Release Player.
        private void releasePlayer(){
            exoPlayer.release();
        }
    }



}
