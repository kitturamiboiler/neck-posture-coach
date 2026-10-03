package com.google.mlkit.vision.demo.java.neckposture;

import java.util.Arrays;

/** Geometry only; identifying anatomical landmarks is a separate step. */
final class CvaMath {
  private CvaMath() {}

  static double angle(double ex, double ey, double bx, double by) {
    if (!finite(ex) || !finite(ey) || !finite(bx) || !finite(by)) return Double.NaN;
    double dx = Math.abs(ex - bx);
    double dy = by - ey;
    return dy <= 0 || Math.hypot(dx, dy) < 1
        ? Double.NaN : Math.toDegrees(Math.atan2(dy, dx));
  }

  static double median(double[] values) {
    if (values.length != 3) throw new IllegalArgumentException("Three measurements required");
    double[] sorted = values.clone();
    for (double value : sorted) {
      if (!finite(value) || value <= 0 || value > 90) {
        throw new IllegalArgumentException("Invalid measurement");
      }
    }
    Arrays.sort(sorted);
    return sorted[1];
  }

  // ponytail: provisional repeatability gate; calibrate with a supervised repeat-photo study.
  static boolean repeatable(double[] values) {
    median(values);
    double min = Math.min(values[0], Math.min(values[1], values[2]));
    double max = Math.max(values[0], Math.max(values[1], values[2]));
    return max - min <= 5.0;
  }

  private static boolean finite(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value);
  }
}
