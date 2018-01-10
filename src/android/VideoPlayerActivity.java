package com.comrat.CordovaVideoPlayerActivity;

import android.content.Intent;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import org.apache.cordova.*;

public class VideoPlayerActivity extends CordovaActivity {
    protected static final String LOG_TAG = "VideoPlayer";

    ProgressDialog pDialog;
    VideoView videoview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                url = null;
            } else {
                url = extras.getString("VIDEO_URL");
            }
        } else {
            url = (String)savedInstanceState.getSerializable("VIDEO_URL");
        }

        Log.d(LOG_TAG, "PlayUrl: " + url);

        setContentView(this.getResources().getIdentifier("content_activity_test", "layout", this.getPackageName()));
        videoview = (VideoView)findViewById(this.getResources().getIdentifier("activity_test_viewvideo", "id", this.getPackageName()));

        pDialog = new ProgressDialog(VideoPlayerActivity.this);
        pDialog.setMessage("Buffering...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();

        try {
            /*MediaController mediacontroller = new MediaController(VideoPlayerActivity.this);
            mediacontroller.setAnchorView(videoview);*/
            Uri video = Uri.parse(url);
            //videoview.setMediaController(mediacontroller);
            videoview.setVideoURI(video);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoview.requestFocus();
        videoview.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                pDialog.dismiss();
                videoview.start();
            }
        });

        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //Log.d(LOG_TAG, "Video completed");
                VideoPlayerActivity.this.finish();
            }
        });
    }
}