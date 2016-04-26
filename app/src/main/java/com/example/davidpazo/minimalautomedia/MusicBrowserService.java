package com.example.davidpazo.minimalautomedia;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserServiceCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicBrowserService extends MediaBrowserService {

    private MediaSession mSession;
    private List<MediaMetadata> mMusic;
    private MediaPlayer mMediaPlayer;
    private MediaMetadata mCurrentTrack;
    @Override
    public void onCreate() {
        super.onCreate();

        //Create entries for two songs
        mMusic = new ArrayList<MediaMetadata>();
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Music 1")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist 1")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 30000)
                .build());
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Music 2")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist 3")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 30000)
                .build());
        //Responsable de playing back the music
        mMediaPlayer = new MediaPlayer();

        //MediaSession object
        mSession = new MediaSession(this, "MyMusicService");
        //Callbacks to handle events from the user(play, pause, search)
        mSession.setCallback(new MediaSession.Callback() {
            //Play song from a media id value
            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                for (MediaMetadata item : mMusic) {
                    if (item.getDescription().getMediaId().equals(mediaId)) {
                        mCurrentTrack = item;
                        break;
                    }
                }
                handlePlay();
            }

            @Override
            public void onPlay() {
                if (mCurrentTrack == null) {
                    mCurrentTrack = mMusic.get(0);
                    handlePlay();
                } else {
                    mMediaPlayer.start();
                    mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
                }
            }
            //Pause button pressed by user
            @Override
            public void onPause() {
                //Pause the music playback
                mMediaPlayer.pause();
                //Update the UI to show we are paused
                mSession.setPlaybackState(buildState(PlaybackState.STATE_PAUSED));
            }
        });
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);
        setSessionToken(mSession.getSessionToken());
    }
    //Helper method to start playing a track
    private PlaybackState buildState(int state) {
        //Hard code the current position and length of track to simplify the code
        return new PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackState.ACTION_SKIP_TO_NEXT
                    | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                    | PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state,mMediaPlayer.getCurrentPosition(), 1, SystemClock.elapsedRealtime())
                .build();
    }
    
    private void handlePlay(){
        //Handle the MediaSession state
        mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
        //Update the session with the track meta-data
        mSession.setMetadata(mCurrentTrack);
        try{
            //Pass the music url to the MediaPlayer
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(MusicBrowserService.this,
                    Uri.parse(mCurrentTrack.getDescription().getMediaId()));
        }catch(IOException e){
            e.printStackTrace();
        }
        //Set a callback for when the music is ready to be played
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            @Override
            public void onPrepared(MediaPlayer mp){
                //Start the playback now that the track is ready
                mp.start();
            }
        });
        mMediaPlayer.prepareAsync();
    }
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints){
        //Perform any checks here to only allow Android Auto, Android Wear, etc to get access
        //Return a root node since we want to allow the client to browse our collection
        return new BrowserRoot("ROOT",null);
    }
    //Build up a tree of al available music from the list prepared earlier
    @Override
    public void onLoadChildren(String parentId, MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result){
        List<MediaBrowser.MediaItem> list = new ArrayList<MediaBrowser.MediaItem>();
        for(MediaMetadata m: mMusic){
            list.add(new MediaBrowser.MediaItem(m.getDescription(),MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(list);
    }
}
