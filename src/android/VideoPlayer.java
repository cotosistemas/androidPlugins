package com.cotosistemas.cordova.plugins;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout; 
import android.widget.VideoView;
import android.widget.TextView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;
import android.view.MotionEvent;
import android.view.View;

public class VideoPlayer extends CordovaPlugin implements OnCompletionListener, OnPreparedListener, OnErrorListener, OnDismissListener {

    protected static final String LOG_TAG = "VideoPlayer";
    protected static final String ASSETS = "/android_asset/";
    private CallbackContext callbackContext = null;
    private Dialog dialog;
    private VideoView videoView;
    private MediaPlayer player;		

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action        The action to execute.
     * @param args          JSONArray of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("play")) {
            this.callbackContext = callbackContext;

            CordovaResourceApi resourceApi = webView.getResourceApi();
            String target = args.getString(0);
            final JSONObject options = args.getJSONObject(1);

            String fileUriStr;
            try {
                Uri targetUri = resourceApi.remapUri(Uri.parse(target));
                fileUriStr = targetUri.toString();
            } catch (IllegalArgumentException e) {
                fileUriStr = target;
            }

            Log.v(LOG_TAG, fileUriStr);

            final String path = stripFileProtocol(fileUriStr);

            // Create dialog in new thread
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    openVideoDialog(path, options);
                }
            });

            // Don't return any result now
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            callbackContext = null;

            return true;
        }
        else if (action.equals("close")) {
            if (dialog != null) {
                if(player.isPlaying()) {
                    player.stop();
                }
                player.release();
                dialog.dismiss();
            }

            if (callbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                result.setKeepCallback(false); // release status callback in JS side
                callbackContext.sendPluginResult(result);
                callbackContext = null;
            }

            return true;
        }
        return false;
    }

    /**
     * Removes the "file://" prefix from the given URI string, if applicable.
     * If the given URI string doesn't have a "file://" prefix, it is returned unchanged.
     *
     * @param uriString the URI string to operate on
     * @return a path without the "file://" prefix
     */
    public static String stripFileProtocol(String uriString) {
        if (uriString.startsWith("file://")) {
            return Uri.parse(uriString).getPath();
        }
        return uriString;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void openVideoDialog(String path, JSONObject options) {
        // Let's create the main dialog
        dialog = new Dialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
        dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(this);

		dialog.setContentView(cordova.getActivity().getResources().getIdentifier("videoplayer_layout", "layout", cordova.getActivity().getPackageName()));      
		
		videoView = (VideoView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("layout_video_viewvideo", "id", cordova.getActivity().getPackageName()));
		
		RelativeLayout rlVideo = (RelativeLayout) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_layout_container", "id", cordova.getActivity().getPackageName()));
		
		
		rlVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
				videoView.stopPlayback();
				dialog.dismiss();
                return false;
            }
        });
		
		videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {              
				videoView.stopPlayback();
				dialog.dismiss();
            }
        });
		
		Uri uri= Uri.parse(path);
        videoView.setVideoURI(uri);
        videoView.start();
		
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        //dialog.setContentView(main);
		
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, "MediaPlayer.onError(" + what + ", " + extra + ")");
        if(mp.isPlaying()) {
            mp.stop();
        }
        mp.release();
        dialog.dismiss();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG_TAG, "MediaPlayer completed");
        mp.release();
        dialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(LOG_TAG, "Dialog dismissed");
        if (callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(false); // release status callback in JS side
            callbackContext.sendPluginResult(result);
            callbackContext = null;
        }
    }
}
