package com.google.mlkit.vision.demo.java.neckposture;

import android.graphics.PointF;

final class NeckPostureAngle {

  // User-selected comparison only; not a validated diagnostic cutoff for shoulder landmarks.
  static final double REFERENCE_ANGLE_DEGREES = 50.0;

  private NeckPostureAngle() {}

  static boolean isBelowReference(double angle) {
    return angle > 0 && angle < REFERENCE_ANGLE_DEGREES;
  }

  static boolean isInsideImage(PointF point, int width, int height) {
    return isFinite(point.x) && isFinite(point.y)
        && point.x >= 0 && point.x < width && point.y >= 0 && point.y < height;
  }

  static double calculate(PointF ear, PointF shoulder) {
    if (!isFinite(ear.x) || !isFinite(ear.y) || !isFinite(shoulder.x) || !isFinite(shoulder.y)) {
      return Double.NaN;
    }

    double dx = Math.abs(ear.x - shoulder.x);
    double dy = shoulder.y - ear.y;
    if (dy <= 0 || Math.hypot(dx, dy) < 1) {
      return Double.NaN;
    }
    return Math.toDegrees(Math.atan2(dy, dx));
  }

  private static boolean isFinite(float value) {
    return !Float.isNaN(value) && !Float.isInfinite(value);
  }
}
