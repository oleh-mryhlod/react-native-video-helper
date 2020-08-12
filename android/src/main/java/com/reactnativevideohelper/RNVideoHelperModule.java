
package com.reactnativevideohelper;

import android.net.Uri;
import android.util.Log;
import android.os.AsyncTask.Status;
import android.support.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativevideohelper.video.VideoCompressionTask.VideoCompressionListener;
import com.reactnativevideohelper.video.VideoCompressionTask;
import java.io.File;
import java.util.UUID;

public class RNVideoHelperModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNVideoHelper";
  private final ReactApplicationContext reactContext;
  private VideoCompressionTask videoCompressTask = null;

  public RNVideoHelperModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private void sendProgress(ReactContext reactContext, float progress) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("progress", progress);
  }

  @Override
  public String getName() {
    return "RNVideoHelper";
  }

  @ReactMethod
  public void cancelCompress() {
    cancelExistingTaskIfExists();
  }

  private void cancelExistingTaskIfExists() {
    /* Cancel any existing task */
    if (videoCompressTask != null && videoCompressTask.getStatus() == Status.RUNNING) {
      videoCompressTask.cancel();
    }
  }

  @ReactMethod
  public void compress(String source, ReadableMap options, final Promise pm) {
    final String inputUri = Uri.parse(source).getPath();
    final File outputDir = reactContext.getCacheDir();

    final String outputUri = String.format("%s/%s.mp4", outputDir.getPath(), UUID.randomUUID().toString());

    final String quality = options.hasKey("quality") ? options.getString("quality") : "";
    final long startTime = options.hasKey("startTime") ? (long)options.getDouble("startTime") : -1;
    final long endTime = options.hasKey("endTime") ? (long)options.getDouble("endTime") : -1;
    final int defaultOrientation = options.hasKey("defaultOrientation") ? (int)options.getInt("defaultOrientation") : 0;

    try {
      videoCompressTask = new VideoCompressionTask(
          inputUri,
          outputUri,
          quality,
          startTime,
          endTime,
          createListener(pm, outputUri),
          defaultOrientation
      );
      videoCompressTask.execute();
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  @NonNull
  private VideoCompressionListener createListener(final Promise pm, final String outputUri) {
    return new VideoCompressionListener() {
      @Override
      public void onStart() {
        //Start Compress
        Log.d("INFO", "Compression started");
      }

      @Override
      public void onSuccess() {
        //Finish successfully
        pm.resolve(outputUri);
      }

      @Override
      public void onFail() {
        //Failed
        pm.reject("ERROR", "Failed to compress video");
      }

      @Override
      public void onProgress(float percent) {
        sendProgress(reactContext, percent / 100);
      }
    };
  }
}