package com.google.mlkit.vision.demo.java.neckposture;

/** Capture timing only; face framing is not anatomical landmark verification. */
final class CaptureReadiness {
  static final float LEFT = .10f, TOP = .06f, RIGHT = .90f, BOTTOM = .58f;
  static final long FRESH_MS = 1200;
  private long since = -1, last = -1;

  static boolean framed(float left, float top, float right, float bottom) {
    return left >= LEFT && top >= TOP && right <= RIGHT && bottom <= BOTTOM
        && right - left >= .14f && bottom - top >= .14f;
  }

  boolean update(long now, boolean level, boolean framed) {
    if (!level || !framed) { reset(); return false; }
    if (since < 0 || last < 0 || now - last > FRESH_MS || now < last) since = now;
    last = now;
    return now - since >= 2000;
  }

  void reset() { since = -1; last = -1; }
}
