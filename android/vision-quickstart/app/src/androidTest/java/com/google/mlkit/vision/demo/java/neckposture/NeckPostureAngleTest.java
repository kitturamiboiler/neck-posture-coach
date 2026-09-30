package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.PointF;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.face.FaceLandmark;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NeckPostureAngleTest {

  @Test
  public void calculatesAngleFromHorizontal() {
    assertEquals(90.0, NeckPostureAngle.calculate(new PointF(10, 10), new PointF(10, 110)), 0.01);
    assertEquals(45.0, NeckPostureAngle.calculate(new PointF(110, 10), new PointF(10, 110)), 0.01);
    assertEquals(45.0, NeckPostureAngle.calculate(new PointF(10, 10), new PointF(110, 110)), 0.01);
    assertTrue(Double.isNaN(NeckPostureAngle.calculate(new PointF(10, 120), new PointF(10, 110))));
  }

  @Test
  public void rejectsInvalidAndOutOfFramePoints() {
    assertTrue(NeckPostureAngle.isInsideImage(new PointF(0, 0), 640, 480));
    assertFalse(NeckPostureAngle.isInsideImage(new PointF(640, 20), 640, 480));
    assertFalse(NeckPostureAngle.isInsideImage(new PointF(20, 480), 640, 480));
    assertFalse(NeckPostureAngle.isInsideImage(new PointF(-1, 20), 640, 480));
    assertFalse(NeckPostureAngle.isInsideImage(new PointF(Float.NaN, 20), 640, 480));
    assertFalse(NeckPostureAngle.isInsideImage(new PointF(20, Float.POSITIVE_INFINITY), 640, 480));
    assertTrue(Double.isNaN(NeckPostureAngle.calculate(new PointF(10, 10), new PointF(10, 10))));
  }

  @Test
  public void comparesReferenceWithoutDiagnosingInvalidMeasurements() {
    assertTrue(NeckPostureAngle.isBelowReference(49.9));
    assertFalse(NeckPostureAngle.isBelowReference(50.0));
    assertFalse(NeckPostureAngle.isBelowReference(50.1));
    assertFalse(NeckPostureAngle.isBelowReference(Double.NaN));
    assertFalse(NeckPostureAngle.isBelowReference(Double.POSITIVE_INFINITY));
    assertFalse(NeckPostureAngle.isBelowReference(-1));
    assertFalse(NeckPostureAngle.isBelowReference(0));
    assertFalse(NeckPostureProcessor.Measurement.invalid("missing").isValid());
  }

  @Test
  public void selectsSameSideFromYawInsteadOfEarCount() {
    assertEquals(-1, NeckPostureProcessor.sideForYaw(-60));
    assertEquals(1, NeckPostureProcessor.sideForYaw(60));
    assertEquals(-1, NeckPostureProcessor.sideForYaw(-36));
    assertEquals(1, NeckPostureProcessor.sideForYaw(36));
    assertEquals(0, NeckPostureProcessor.sideForYaw(35.9f));
    assertEquals(0, NeckPostureProcessor.sideForYaw(-35.9f));
    assertEquals(0, NeckPostureProcessor.sideForYaw(0));
    assertEquals(0, NeckPostureProcessor.sideForYaw(Float.NaN));
    assertEquals(0, NeckPostureProcessor.sideForYaw(Float.POSITIVE_INFINITY));
    assertEquals(FaceLandmark.RIGHT_EAR,
        NeckPostureProcessor.faceEarTypeForPoseSide(NeckPostureProcessor.sideForYaw(-73.1f)));
    assertEquals(FaceLandmark.LEFT_EAR,
        NeckPostureProcessor.faceEarTypeForPoseSide(NeckPostureProcessor.sideForYaw(83.2f)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void frontalFaceHasNoSelectedEar() {
    NeckPostureProcessor.faceEarTypeForPoseSide(0);
  }

  @Test
  public void manualLoweringPreservesRawPointsAndRejectsOutOfFrameAdjustment() {
    NeckPostureProcessor.Measurement raw = NeckPostureProcessor.Measurement.valid(
        new PointF(400, 100), new PointF(200, 250),
        NeckPostureAngle.calculate(new PointF(400, 100), new PointF(200, 250)), -1);
    NeckPostureProcessor.Measurement adjusted = NeckPostureProcessor.adjustShoulder(raw, 50, 640, 480);
    assertTrue(adjusted.isValid());
    assertTrue(adjusted.isAdjusted());
    assertEquals(200f, adjusted.shoulder.x, 0f);
    assertEquals(300f, adjusted.shoulder.y, 0f);
    assertEquals(250f, raw.shoulder.y, 0f);
    assertEquals(raw.angle, adjusted.rawAngle, 0.001);
    assertEquals(45.0, adjusted.angle, 0.001);
    assertTrue(adjusted.angle > raw.angle);
    assertFalse(NeckPostureProcessor.adjustShoulder(raw, 0, 640, 480).isAdjusted());
    assertFalse(NeckPostureProcessor.adjustShoulder(raw, 50, 640, 300).isValid());
    assertFalse(NeckPostureProcessor.adjustShoulder(raw, -1, 640, 480).isValid());
    assertFalse(NeckPostureProcessor.adjustShoulder(raw, 81, 640, 480).isValid());
    assertFalse(NeckPostureProcessor.adjustShoulder(
        NeckPostureProcessor.Measurement.invalid("missing"), 50, 640, 480).isValid());
  }

  @Test
  public void manualAxesMoveButOriginalConnectionRemainsVisible() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      GraphicOverlay overlay = new GraphicOverlay(
          InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
      overlay.layout(0, 0, 640, 480);
      overlay.setImageSourceInfo(640, 480, false);
      NeckPostureProcessor.Measurement raw = NeckPostureProcessor.Measurement.valid(
          new PointF(400, 100), new PointF(200, 250), 36.87, -1);
      overlay.add(new NeckPostureGraphic(overlay, null,
          NeckPostureProcessor.adjustShoulder(raw, 50, 640, 480)));
      Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
      overlay.draw(new Canvas(bitmap));
      assertEquals(Color.CYAN, bitmap.getPixel(50, 300));
      assertEquals(Color.TRANSPARENT, bitmap.getPixel(50, 250));
      assertEquals(Color.WHITE, bitmap.getPixel(300, 200));
      assertEquals(Color.LTGRAY, bitmap.getPixel(300, 175));
      bitmap.recycle();
    });
  }

  @Test
  public void drawsAxesConnectionAndIncludedArcWithMirroring() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      for (boolean mirrored : new boolean[] {false, true}) {
        GraphicOverlay overlay = new GraphicOverlay(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
        overlay.layout(0, 0, 640, 480);
        overlay.setImageSourceInfo(640, 480, mirrored);
        NeckPostureProcessor.Measurement measurement = NeckPostureProcessor.Measurement.valid(
            new PointF(420, 120), new PointF(220, 320), 45.0, -1);
        overlay.add(new NeckPostureGraphic(overlay, null, measurement));
        Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        overlay.draw(new Canvas(bitmap));
        assertEquals(Color.CYAN, bitmap.getPixel(50, 320));
        assertEquals(Color.YELLOW, bitmap.getPixel(mirrored ? 420 : 220, 50));
        assertEquals(Color.WHITE, bitmap.getPixel(320, 220));
        assertEquals(Color.rgb(255, 138, 101), bitmap.getPixel(mirrored ? 355 : 285, 293));
        // The arc must stay above the shoulder and on the ear-facing side.
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(mirrored ? 485 : 155, 293));
        bitmap.recycle();
      }
    });
  }
}
