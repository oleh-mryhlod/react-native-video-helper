
package com.reactnativevideohelper;

import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor;
import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
import com.abedelazizshe.lightcompressorlibrary.CompressionListener;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;

public class RNVideoHelperModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNVideoHelper";
  private final ReactApplicationContext reactContext;

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
    VideoCompressor.INSTANCE.cancel();
  }

  @ReactMethod
  public void compress(String source, ReadableMap options, final Promise pm) {
    final String inputUri = Uri.parse(source).getPath();
    final File outputDir = reactContext.getCacheDir();

    final String outputUri = String.format("%s/%s.mp4", outputDir.getPath(), UUID.randomUUID().toString());

    final String quality = options.hasKey("quality") ? options.getString("quality") : "";
    final VideoQuality videoQuality;
    switch (quality) {
      case "high":
        videoQuality = VideoQuality.HIGH;
        break;
      case "medium":
        videoQuality = VideoQuality.MEDIUM;
        break;
      default:
        videoQuality = VideoQuality.LOW;
    };

    final boolean isMinBitRateEnabled = options.hasKey("isMinBitRateEnabled") && options.getBoolean("isMinBitRateEnabled");
    final boolean keepOriginalResolution = options.hasKey("isMinBitRateEnabled") && options.getBoolean("isMinBitRateEnabled");

    try {
      VideoCompressor.INSTANCE.start(
              inputUri,
              outputUri,
              createListener(pm, outputUri),
              videoQuality,
              isMinBitRateEnabled,
              keepOriginalResolution);
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  private CompressionListener createListener(final Promise pm, final String outputUri) {
    return new CompressionListener() {
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
      public void onFailure(@NotNull String failureMessage) {
        //Failed
        pm.reject("ERROR", failureMessage);
      }

      @Override
      public void onProgress(float percent) {
        sendProgress(reactContext, percent / 100);
      }

      @Override
      public void onCancelled() {
        // Compression canceled
        Log.d("INFO", "Compression canceled");
      }
    };
  }
}