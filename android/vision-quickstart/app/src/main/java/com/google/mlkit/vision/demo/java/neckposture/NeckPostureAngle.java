package com.google.mlkit.vision.demo.java.neckposture;

import android.graphics.PointF;

final class NeckPostureAngle {

  private NeckPostureAngle() {}

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
