package com.google.mlkit.vision.demo.java.neckposture;

public final class CaptureReadinessCheck {
  public static void main(String[] args) {
    CaptureReadiness gate = new CaptureReadiness();
    check(CaptureReadiness.framed(.2f, .1f, .7f, .5f), "face fits");
    check(!CaptureReadiness.framed(0, .1f, .7f, .5f), "outside");
    check(!CaptureReadiness.framed(.2f, .1f, .25f, .15f), "too small");
    check(!CaptureReadiness.framed(Float.NaN, .1f, .7f, .5f), "invalid coordinates");
    check(!gate.update(0, true, true), "wait first");
    check(!gate.update(1000, true, true), "wait second");
    check(gate.update(2000, true, true), "two second hold");
    check(!gate.update(2100, false, true), "tilt resets");
    check(!gate.update(2200, true, true), "fresh hold after tilt");
    check(!gate.update(2300, true, false), "frame loss resets");
    check(!gate.update(2400, true, true), "fresh hold after frame loss");
    check(!gate.update(5000, true, true), "stale frames cannot finish");
    check(!gate.update(4000, true, true), "clock reversal resets");
    gate.reset();
    check(!gate.update(6000, true, true), "cancel resets");
    System.out.println("Capture readiness checks passed (14 checks)");
  }
  private static void check(boolean result, String description) {
    if (!result) throw new AssertionError(description);
  }
}
