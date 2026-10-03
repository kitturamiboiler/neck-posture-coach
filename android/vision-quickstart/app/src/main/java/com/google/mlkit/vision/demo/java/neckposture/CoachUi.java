package com.google.mlkit.vision.demo.java.neckposture;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

final class CoachUi {
  static final int INK = Color.rgb(29, 40, 39);
  static final int GREEN = Color.rgb(23, 109, 84);
  static final int MUTED = Color.rgb(88, 102, 100);
  private CoachUi() {}

  static int dp(Context context, int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  static LinearLayout page(Activity activity, String title) {
    ScrollView scroll = new ScrollView(activity);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(Color.rgb(248, 250, 249));
    LinearLayout container = new LinearLayout(activity);
    container.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    scroll.addView(container);
    LinearLayout body = new LinearLayout(activity);
    body.setOrientation(LinearLayout.VERTICAL);
    int width = Math.min(activity.getResources().getDisplayMetrics().widthPixels, dp(activity, 760));
    container.addView(body, new LinearLayout.LayoutParams(width, -2));
    body.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 32));
    activity.setContentView(scroll);
    text(body, title, 25, true);
    return body;
  }

  static TextView text(LinearLayout parent, String value, int size, boolean bold) {
    TextView view = new TextView(parent.getContext());
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(bold ? INK : MUTED);
    if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    view.setPadding(0, dp(parent.getContext(), 8), 0, dp(parent.getContext(), 8));
    parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    return view;
  }

  static Button button(LinearLayout parent, String label, int icon, Runnable action) {
    Button button = new Button(parent.getContext());
    button.setText(label);
    button.setAllCaps(false);
    button.setTextColor(GREEN);
    button.setMinHeight(dp(parent.getContext(), 52));
    if (icon != 0) {
      button.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
      button.setCompoundDrawablePadding(dp(parent.getContext(), 12));
    }
    button.setOnClickListener(v -> action.run());
    parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
    return button;
  }

  static RadioGroup options(LinearLayout parent, String[] labels, int selected) {
    RadioGroup group = new RadioGroup(parent.getContext());
    for (int i = 0; i < labels.length; i++) {
      RadioButton radio = new RadioButton(parent.getContext());
      radio.setId(View.generateViewId());
      radio.setText(labels[i]);
      radio.setTextColor(INK);
      radio.setMinHeight(dp(parent.getContext(), 48));
      radio.setButtonTintList(ColorStateList.valueOf(GREEN));
      group.addView(radio);
      if (i == selected) group.check(radio.getId());
    }
    parent.addView(group);
    return group;
  }

  static int selected(RadioGroup group) {
    return group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId()));
  }

  static void rule(LinearLayout parent) {
    View line = new View(parent.getContext());
    line.setBackgroundColor(Color.rgb(217, 226, 222));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(parent.getContext(), 1));
    params.topMargin = dp(parent.getContext(), 16);
    params.bottomMargin = dp(parent.getContext(), 12);
    parent.addView(line, params);
  }
}
