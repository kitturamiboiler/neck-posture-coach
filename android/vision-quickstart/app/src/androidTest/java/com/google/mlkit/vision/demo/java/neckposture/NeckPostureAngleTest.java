package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.PointF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NeckPostureAngleTest {

  @Test
  public void calculatesAngleFromHorizontal() {
    assertEquals(90.0, NeckPostureAngle.calculate(new PointF(10, 10), new PointF(10, 110)), 0.01);
    assertEquals(45.0, NeckPostureAngle.calculate(new PointF(110, 10), new PointF(10, 110)), 0.01);
    assertTrue(Double.isNaN(NeckPostureAngle.calculate(new PointF(10, 120), new PointF(10, 110))));
  }
}
