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
import android.widget.ImageView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import android.webkit.WebView;
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
	private ImageView imageViewHeader, imageViewFooter;
    private MediaPlayer player;	
	private String[] listaVideos;
	private Integer indiceVideo;
	private Boolean hasToLoop;
	private String imageHeaderPath, imageFooterPath;
	
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
			hasToLoop = Boolean.valueOf(args.getString(1));
			imageHeaderPath = args.getString(2);
			imageFooterPath = args.getString(3);
            final JSONObject options = args.getJSONObject(4);

            String fileUriStr;
            try {
                Uri targetUri = resourceApi.remapUri(Uri.parse(target));
                fileUriStr = targetUri.toString();
            } catch (IllegalArgumentException e) {
                fileUriStr = target;
            }

            Log.v(LOG_TAG, fileUriStr);
			indiceVideo = 0;
			
			String urls="";
			listaVideos = fileUriStr.split(",");
			String videoUrl="";
			if(listaVideos.length > 1){
				videoUrl = listaVideos[indiceVideo++];											
			}else{
				videoUrl = listaVideos[indiceVideo++];			
			}
			
            final String path = stripFileProtocol(videoUrl);			
			
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
        }else if (action.equals("rotate")) {
			final String rotation = args.getString(0);
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    rotateView(rotation);
                }
            });

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
    protected void rotateView(String rotation) {
		 if (dialog != null) {	
			imageViewHeader = (ImageView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("layout_video_imageview_header", "id", cordova.getActivity().getPackageName()));
			imageViewFooter = (ImageView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("layout_video_imageview_footer", "id", cordova.getActivity().getPackageName()));			
			
			if(rotation.equals("landscape")){
				imageViewHeader.setVisibility(View.GONE);
				imageViewFooter.setVisibility(View.GONE);			
			}else{
				imageViewHeader.setVisibility(View.VISIBLE);
				imageViewFooter.setVisibility(View.VISIBLE);
			}		
		}		
	}
	
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void openVideoDialog(String path, JSONObject options) {
        // Let's create the main dialog
		if(videoView == null || (videoView != null && !videoView.isPlaying()) ){
			dialog = new Dialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
			dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setCancelable(true);
			dialog.setOnDismissListener(this);
			dialog.setContentView(cordova.getActivity().getResources().getIdentifier("videoplayer_layout", "layout", cordova.getActivity().getPackageName()));      
			
			videoView = (VideoView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("layout_video_viewvideo", "id", cordova.getActivity().getPackageName()));
			
			RelativeLayout rlVideo = (RelativeLayout) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_layout_container", "id", cordova.getActivity().getPackageName()));
			
			WebView imageHeader = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_header", "id", cordova.getActivity().getPackageName()));
			
			if(imageHeaderPath != null && imageHeaderPath.equals("")){
				imageHeader.loadDataWithBaseURL(null, "<html><head></head><body><table style=\"width:100%; height:100%;\"><tr><td style=\"vertical-align:middle;\"><img src=\"" + imageHeaderPath + "\"></td></tr></table></body></html>", "html/css", "utf-8", null);
			}
			
			WebView imageFooter = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_header", "id", cordova.getActivity().getPackageName()));
			
			if(imageFooterPath != null && imageFooterPath.equals("")){
				imageFooter.loadDataWithBaseURL(null, "<html><head></head><body><table style=\"width:100%; height:100%;\"><tr><td style=\"vertical-align:middle;\"><img src=\"" + imageFooterPath + "\"></td></tr></table></body></html>", "html/css", "utf-8", null);
			}
			
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
					if(indiceVideo == listaVideos.length){
						if(!hasToLoop){
							videoView.stopPlayback();
							dialog.dismiss();
						}else{
							indiceVideo = 0;
							Uri uri= Uri.parse(listaVideos[indiceVideo++]);
							videoView.setVideoURI(uri);
							videoView.start();
						}
					}else{
						Uri uri= Uri.parse(listaVideos[indiceVideo++]);
						videoView.setVideoURI(uri);
						videoView.start();
					}
				}
			});			
			Log.v(LOG_TAG, path);
			Uri uri= Uri.parse(path);
			videoView.setVideoURI(uri);
			videoView.start();
			
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(dialog.getWindow().getAttributes());
			lp.width = WindowManager.LayoutParams.MATCH_PARENT;
			lp.height = WindowManager.LayoutParams.MATCH_PARENT;
			dialog.show();
			dialog.getWindow().setAttributes(lp);
			dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			
			View decorView = dialog.getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
			decorView.setSystemUiVisibility(uiOptions);
		}
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
