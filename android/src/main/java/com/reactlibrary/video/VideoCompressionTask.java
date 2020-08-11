package com.reactnativevideohelper.video;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import com.reactnativevideohelper.video.MediaController.CompressProgressListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoCompressionTask extends AsyncTask<Void, Float, Boolean> {

  private static final String TAG = VideoCompressionTask.class.getSimpleName();

  private final String srcPath;
  private final String destPath;
  private final VideoCompressionListener listener;
  private final String quality;
  private final long startTime;
  private final long endTime;
  private int defaultOrientation;
  private final AtomicBoolean isCancelled = new AtomicBoolean(false);

  public VideoCompressionTask(
      final String srcPath,
      final String destPath,
      final String quality,
      final long startTime,
      final long endTime,
      final VideoCompressionListener listener,
      final int defaultOrientation) {
    this.srcPath = srcPath;
    this.destPath = destPath;
    this.listener = listener;
    this.quality = quality;
    this.startTime = startTime;
    this.endTime = endTime;
    this.defaultOrientation = defaultOrientation;
  }

  static int toMediaControllerQuality(final String quality) {
    if ("high".equals(quality)) {
      return MediaController.COMPRESS_QUALITY_HIGH;
    } else if ("medium".equals(quality)) {
      return MediaController.COMPRESS_QUALITY_MEDIUM;
    }
    return MediaController.COMPRESS_QUALITY_LOW;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    if (listener != null) {
      listener.onStart();
    }
  }

  @Override
  protected Boolean doInBackground(Void... ignored) {
    deleteDestPathFileIfExists();

    MediaController media = MediaController.getInstance();
    media.SetDefaultOrientation(defaultOrientation);

    return media.convertVideo(
        srcPath,
        destPath,
        toMediaControllerQuality(quality),
        startTime,
        endTime,
        getListener(),
        isCancelled);
  }

  private boolean deleteDestPathFileIfExists() {
    try {
      final File file = new File(destPath);
      if (file.exists()) {
        Log.w(TAG, "deleting existing file");
        return file.delete();
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to delete destPath", e);
    }
    return false;
  }

  @NonNull
  private CompressProgressListener getListener() {
    return new CompressProgressListener() {
      @Override
      public void onProgress(float percent) {
        publishProgress(percent);
      }
    };
  }

  @Override
  protected void onProgressUpdate(Float... percent) {
    super.onProgressUpdate(percent);
    if (listener != null) {
      listener.onProgress(percent[0]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
    if (listener != null) {
      if (result) {
        listener.onSuccess();
      } else {
        listener.onFail();
      }
    }
  }

  /**
   * Overloaded cancel method which interrupts ongoing task.
   */
  public void cancel() {
    Log.w(TAG, "Canceling video compression task.");
    isCancelled.set(true);

    /* Interrupt the compression task */
    super.cancel(true);
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
    deleteDestPathFileIfExists();
  }

  public interface VideoCompressionListener {

    void onStart();

    void onSuccess();

    void onFail();

    void onProgress(float percent);
  }
}