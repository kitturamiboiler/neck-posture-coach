package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CoachUiTest {
  @Test public void iconAndLabelAreCenteredAtPhoneAndTabletWidths() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
      for (int width : new int[]{288, 328, 568}) {
        LinearLayout parent = new LinearLayout(context);
        parent.setOrientation(LinearLayout.VERTICAL);
        Button button = CoachUi.button(parent, "운동 시작", android.R.drawable.ic_media_play, () -> {});
        CoachUi.styleButton(button, true);
        layout(parent, CoachUi.dp(context, width));
        Drawable icon = button.getCompoundDrawablesRelative()[0];
        float contentWidth = button.getPaint().measureText(button.getText().toString())
            + icon.getBounds().width() + button.getCompoundDrawablePadding();
        assertEquals(button.getWidth() / 2f, button.getPaddingLeft() + contentWidth / 2f, 1.5f);
        assertTrue(button.getHeight() >= CoachUi.dp(context, 48));
        button.setText("계속하기");
        layout(parent, CoachUi.dp(context, width));
        contentWidth = button.getPaint().measureText(button.getText().toString())
            + icon.getBounds().width() + button.getCompoundDrawablePadding();
        assertEquals(button.getWidth() / 2f, button.getPaddingLeft() + contentWidth / 2f, 1.5f);
        button.setEnabled(false);
        assertFalse(button.isEnabled());
      }
    });
  }

  @Test public void largeFontChoicesStackAndButtonsWrapWithoutClipping() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
      Configuration config = new Configuration(base.getResources().getConfiguration());
      config.fontScale = 2;
      Context context = base.createConfigurationContext(config);
      LinearLayout parent = new LinearLayout(context);
      parent.setOrientation(LinearLayout.VERTICAL);
      RadioGroup group = CoachUi.options(parent, new String[]{"왼쪽 옆모습", "오른쪽 옆모습"}, 1);
      assertEquals(RadioGroup.VERTICAL, group.getOrientation());
      assertEquals(1, CoachUi.selected(group));
      Button button = CoachUi.button(parent, "저장하지 않고 오늘 화면으로 돌아가기", android.R.drawable.ic_menu_close_clear_cancel, () -> {});
      layout(parent, CoachUi.dp(context, 288));
      assertTrue("A long label must wrap instead of being truncated", button.getLineCount() > 1);
      assertTrue(button.getHeight() >= button.getLayout().getHeight()
          + button.getCompoundPaddingTop() + button.getCompoundPaddingBottom());
    });
  }

  private static void layout(View view, int width) {
    // Padding changes after the first size pass; repeat as the view traversal would.
    for (int i = 0; i < 3; i++) {
      view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
      view.layout(0, 0, width, view.getMeasuredHeight());
    }
  }
}
