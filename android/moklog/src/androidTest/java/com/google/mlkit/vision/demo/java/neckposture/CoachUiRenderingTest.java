package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CoachUiRenderingTest {
  @Test public void attachedButtonKeepsContrastAcrossStateChanges() throws Exception {
    java.util.concurrent.CountDownLatch laidOut = new java.util.concurrent.CountDownLatch(1);
    try (ActivityScenario<CoachUiRenderActivity> scenario = ActivityScenario.launch(CoachUiRenderActivity.class)) {
      scenario.onActivity(activity -> {
        LinearLayout body = new LinearLayout(activity);
        Button button = CoachUi.button(body, "운동 시작", 0, () -> {});
        button.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
          if (r > l && b > t) laidOut.countDown();
        });
        button.setId(android.R.id.button1);
        CoachUi.styleButton(button, true);
        activity.setContentView(body);
      });
      assertTrue("Button must complete layout", laidOut.await(5, java.util.concurrent.TimeUnit.SECONDS));
      scenario.onActivity(activity -> {
        Button button = activity.findViewById(android.R.id.button1);
        assertTrue(button.isAttachedToWindow());
        assertFill(button, CoachUi.GREEN);
        button.setEnabled(false);
        assertFill(button, Color.rgb(238, 241, 239));
        button.setEnabled(true);
        assertFill(button, CoachUi.GREEN);
        CoachUi.styleButton(button, false);
        assertFill(button, Color.WHITE);
        CoachUi.styleButton(button, true);
        assertFill(button, CoachUi.GREEN);
      });
    }
  }
  private static void assertFill(Button button, int expected) {
    button.jumpDrawablesToCurrentState();
    Bitmap bitmap = Bitmap.createBitmap(button.getWidth(), button.getHeight(), Bitmap.Config.ARGB_8888);
    button.draw(new Canvas(bitmap));
    int actual = bitmap.getPixel(button.getWidth() / 2, CoachUi.dp(button.getContext(), 4));
    bitmap.recycle();
    assertEquals("Attached button background", expected, actual);
  }
}
