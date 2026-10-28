package com.google.mlkit.vision.demo.java.neckposture;

import com.google.mlkit.vision.face.FaceLandmark;

/** Preserves the face 16.1.7 ear mapping verified in the original mirrored replay. */
final class FaceLandmarkMapping {
  static int sideForYaw(float yaw) {
    return Float.isNaN(yaw) || Float.isInfinite(yaw) || Math.abs(yaw) > 180 || Math.abs(yaw) < 36
        ? 0 : yaw < 0 ? -1 : 1;
  }
  static int earForSide(int side) {
    if (side == -1) return FaceLandmark.RIGHT_EAR;
    if (side == 1) return FaceLandmark.LEFT_EAR;
    throw new IllegalArgumentException("Side required");
  }
}
