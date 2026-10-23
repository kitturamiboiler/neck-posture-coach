package com.google.mlkit.vision.demo.java.neckposture;

import android.graphics.Bitmap;
import android.graphics.PointF;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

/** Photos live only in memory, including across configuration changes. */
public final class PostureSession extends ViewModel {
  final List<Bitmap> photos = new ArrayList<>();
  final PointF[] ears = new PointF[3];
  final PointF[] bases = new PointF[3];
  final double[] angles = new double[3];
  final int[] quality = new int[3];
  final String[] qualityMessage = new String[3];
  String screen = "home";
  String side = "왼쪽";
  String camera = "후면";
  int index;
  long savedId = -1;
  int routineRemaining;
  boolean routineRunning;
  boolean autoCapture = true;
  boolean captureFeedback = true;

  void clearPhotos() {
    // Bitmap references may still belong to an in-flight ML Kit task; let GC reclaim them.
    photos.clear();
    for (int i = 0; i < 3; i++) {
      ears[i] = null; bases[i] = null; angles[i] = 0;
      quality[i] = 0; qualityMessage[i] = null;
    }
    index = 0;
    savedId = -1;
  }

  String protocol() { return "standing-v1 / " + side + " / " + camera; }
  @Override protected void onCleared() { clearPhotos(); }
}
