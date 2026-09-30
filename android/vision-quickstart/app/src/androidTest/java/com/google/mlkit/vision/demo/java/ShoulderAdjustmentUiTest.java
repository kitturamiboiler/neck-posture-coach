package com.google.mlkit.vision.demo.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.mlkit.vision.demo.R;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShoulderAdjustmentUiTest {
  @Test
  public void adjustmentIsExplicitAndResetsOutsideNeckMode() {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    LivePreviewActivity activity = (LivePreviewActivity) instrumentation.startActivitySync(
        new Intent(instrumentation.getTargetContext(), LivePreviewActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      instrumentation.waitForIdleSync();
      instrumentation.runOnMainSync(() -> {
        SeekBar slider = activity.findViewById(R.id.shoulder_offset);
        assertEquals(View.VISIBLE, activity.findViewById(R.id.shoulder_adjustment).getVisibility());
        assertEquals(0, slider.getProgress());
        slider.setProgress(30);
        assertTrue(((TextView) activity.findViewById(R.id.shoulder_offset_label))
            .getText().toString().contains("30"));
        // Face Detection is the fifth item in the existing sample menu.
        activity.onItemSelected((Spinner) activity.findViewById(R.id.spinner), null, 4, 4);
      });
      instrumentation.waitForIdleSync();
      instrumentation.runOnMainSync(() -> {
        assertEquals(View.GONE, activity.findViewById(R.id.shoulder_adjustment).getVisibility());
        assertEquals(0, ((SeekBar) activity.findViewById(R.id.shoulder_offset)).getProgress());
      });
    } finally {
      instrumentation.runOnMainSync(activity::finish);
    }
  }
}
