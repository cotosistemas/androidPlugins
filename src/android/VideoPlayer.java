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
import android.content.Context;
import android.view.Display;
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
import android.webkit.WebSettings;
import org.json.JSONException;
import org.json.JSONObject;
import android.view.MotionEvent;
import org.json.*;
import android.view.View;

public class VideoPlayer extends CordovaPlugin implements OnCompletionListener, OnPreparedListener, OnErrorListener, OnDismissListener {

    protected static final String LOG_TAG = "VideoPlayer";
    protected static final String ASSETS = "/android_asset/";
    private CallbackContext callbackContext = null;
    private Dialog dialog;
    private VideoView videoView;
	private WebView webViewHeader, webViewFooter;
    private MediaPlayer player;	
	//private String[] listaVideos, listaVideoImagen;
	private Integer indiceVideo;
	private Boolean hasToLoop;
	private String video, imagenHeader, imagenFooter;
	private String imageHeaderPath, imageFooterPath;
	private JSONArray videoArrJson;
	
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
			Log.v(LOG_TAG, target);
			
			JSONObject obj = new JSONObject(target);
		
			videoArrJson = obj.getJSONArray("FilterList");	
			
			hasToLoop = Boolean.valueOf(args.getString(1));
            final JSONObject options = args.getJSONObject(2);
			
			indiceVideo = 0;
			
			video = videoArrJson.getJSONObject(indiceVideo).getString("PathCompleto");					
			imagenHeader = videoArrJson.getJSONObject(indiceVideo).getString("ImageHeaderPath");	
			Log.v(LOG_TAG, imagenHeader);			
			imagenFooter = videoArrJson.getJSONObject(indiceVideo).getString("ImageFooterPath");	
			Log.v(LOG_TAG, imagenFooter);			
			indiceVideo++;
			
			String urls="";
			String videoUrl="";
	
            // Create dialog in new thread
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    openVideoDialog(video, imagenHeader, imagenFooter, options);
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
			webViewHeader = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_header", "id", cordova.getActivity().getPackageName()));
			webViewFooter = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_footer", "id", cordova.getActivity().getPackageName()));			
			
			if(rotation.equals("landscape")){
				webViewHeader.setVisibility(View.GONE);
				webViewFooter.setVisibility(View.GONE);			
			}else{
				webViewHeader.setVisibility(View.VISIBLE);
				webViewFooter.setVisibility(View.VISIBLE);
			}		
		}		
	}
	
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void openVideoDialog(String path, String imageHeader, String imageFooter, JSONObject options) {
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
			
			webViewHeader = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_header", "id", cordova.getActivity().getPackageName()));				
			
			if(imageHeader != null && !imageHeader.equals("")){							
				webViewHeader.loadDataWithBaseURL("file:///android_asset/", "<html>\n" +
					"<body bgcolor=\"white\">\n" +"<table width=\"100%\" height=\"100%\">\n" +"<tr>\n" +"<td align=\"center\" valign=\"center\">\n" +
					"<img src="+imageHeader+">\n" + "</td>\n" + "</tr>\n" + "</table>\n" +
					"</body>", "text/html", "utf-8", "");
				//webViewHeader.loadUrl(imageHeaderPath);
				webViewHeader.getSettings().setLoadWithOverviewMode(true);
				webViewHeader.getSettings().setUseWideViewPort(true);
				webViewHeader.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
			}else{
				webViewHeader.setVisibility(View.GONE);
			}
			
			webViewFooter = (WebView) dialog.findViewById(cordova.getActivity().getResources().getIdentifier("videoplayer_imageview_footer", "id", cordova.getActivity().getPackageName()));
			
			if(imageFooter != null && !imageFooter.equals("")){				
				webViewFooter.loadDataWithBaseURL("file:///android_asset/", "<html>\n" +
					"<body bgcolor=\"white\">\n" +"<table width=\"100%\" height=\"100%\">\n" +"<tr>\n" +"<td align=\"center\" valign=\"center\">\n" +
					"<img src="+imageFooter+">\n" + "</td>\n" + "</tr>\n" + "</table>\n" +
					"</body>", "text/html", "utf-8", "");
				//webViewFooter.loadUrl(imageFooterPath);
				webViewFooter.getSettings().setLoadWithOverviewMode(true);
				webViewFooter.getSettings().setUseWideViewPort(true);
				webViewHeader.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
			}else{
				webViewFooter.setVisibility(View.GONE);
			}
			
			webViewHeader.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					videoView.stopPlayback();
					dialog.dismiss();
					return false;
				}
			});
			
			webViewFooter.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					videoView.stopPlayback();
					dialog.dismiss();
					return false;
				}
			});				
			
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
					if(indiceVideo == videoArrJson.length()){						
						if(!hasToLoop){
							videoView.stopPlayback();
							dialog.dismiss();
						}else{
							indiceVideo = 0;	
							runNextVideo();													
						}
					}else{
						runNextVideo();				
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
	
	public void runNextVideo(){
		try{
			video = videoArrJson.getJSONObject(indiceVideo).getString("PathCompleto");					
			imagenHeader = videoArrJson.getJSONObject(indiceVideo).getString("ImageHeaderPath");								
			imagenFooter = videoArrJson.getJSONObject(indiceVideo).getString("ImageFooterPath");	
			if(imagenHeader != null && !imagenHeader.equals("")){
				webViewHeader.loadDataWithBaseURL("file:///android_asset/", "<html>\n" +
					"<body bgcolor=\"white\">\n" +"<table width=\"100%\" height=\"100%\">\n" +"<tr>\n" 
					+"<td align=\"center\" valign=\"center\">\n" +
					"<img src="+imagenHeader+">\n" + "</td>\n" + "</tr>\n" + "</table>\n" +
					"</body>", "text/html", "utf-8", "");
				//webViewHeader.loadUrl(imagenHeader);
			}else
				webViewHeader.setVisibility(View.GONE);
				
			if(imagenFooter != null && !imagenFooter.equals("")){
				webViewFooter.loadDataWithBaseURL("file:///android_asset/", "<html>\n" +
					"<body bgcolor=\"white\">\n" +"<table width=\"100%\" height=\"100%\">\n" +"<tr>\n" 
					+"<td align=\"center\" valign=\"center\">\n" +
					"<img src="+imagenFooter+">\n" + "</td>\n" + "</tr>\n" + "</table>\n" +
					"</body>", "text/html", "utf-8", "");
				//webViewFooter.loadUrl(imagenFooter);
			}else
				webViewFooter.setVisibility(View.GONE);
			indiceVideo++;							
			Uri uri= Uri.parse(video);
			videoView.setVideoURI(uri);
			videoView.start();
		}catch(Exception ex){			
			ex.printStackTrace();
		}
	}
	
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, "MediaPlayer.onError(" + what + ", " + extra + ")");
        /*if(mp.isPlaying()) {
            mp.stop();
        }*/
        //mp.release();
        dialog.dismiss();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG_TAG, "MediaPlayer completed");
		if(mp != null)
			mp.release();
		if(dialog != null)
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

