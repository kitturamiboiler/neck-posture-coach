package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.*;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaptureGuideTest {
  @Test public void guideExcludesLetterboxingInBothOrientations() {
    RectF portrait = CaptureGuideView.imageRect(600, 400, 300, 400);
    assertEquals(new RectF(150, 0, 450, 400), portrait);
    RectF landscape = CaptureGuideView.imageRect(400, 600, 400, 300);
    assertEquals(new RectF(0, 150, 400, 450), landscape);
    assertEquals(300, portrait.centerX(), .001);
    assertEquals(300, landscape.centerY(), .001);
  }

  @Test public void guideDrawsVisibleBordersAndReadyState() {
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      CaptureGuideView view = new CaptureGuideView(
          androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext());
      view.measure(android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY),
          android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY));
      view.layout(0, 0, 600, 400);
      android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(600, 400,
          android.graphics.Bitmap.Config.ARGB_8888);
      android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
      view.draw(canvas);
      assertEquals(0xffffffff, bitmap.getPixel(300, 24));
      assertEquals(0, bitmap.getPixel(20, 24));
      view.update(300, 400, true);
      bitmap.eraseColor(0);
      view.draw(canvas);
      assertEquals(0xff69f0ae, bitmap.getPixel(300, 24));
      bitmap.recycle();
    });
  }
}
