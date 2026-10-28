package com.google.mlkit.vision.demo.java.neckposture;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.FrameLayout;
import androidx.appcompat.widget.AppCompatButton;

final class CoachUi {
  static final int INK = Color.rgb(29, 40, 39);
  static final int GREEN = Color.rgb(23, 109, 84);
  static final int MUTED = Color.rgb(88, 102, 100);
  static final int BACKGROUND = Color.rgb(248, 250, 249);
  static final int SOFT = Color.rgb(232, 243, 237);
  static final int BORDER = Color.rgb(220, 227, 223);
  private CoachUi() {}

  static int dp(Context context, int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  static LinearLayout page(Activity activity, String title) {
    return page(activity, title, null);
  }

  static LinearLayout page(Activity activity, String title, Runnable back) {
    activity.getWindow().setStatusBarColor(BACKGROUND);
    activity.getWindow().setNavigationBarColor(BACKGROUND);
    activity.getWindow().getDecorView().setSystemUiVisibility(
        (android.os.Build.VERSION.SDK_INT >= 23 ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0)
        | (android.os.Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(BACKGROUND);
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
      androidx.core.graphics.Insets bars = insets.getInsets(
          androidx.core.view.WindowInsetsCompat.Type.systemBars()
              | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
              | androidx.core.view.WindowInsetsCompat.Type.ime());
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return androidx.core.view.WindowInsetsCompat.CONSUMED;
    });
    ScrollView scroll = new ScrollView(activity);
    scroll.setFillViewport(true);
    FrameLayout container = new FrameLayout(activity);
    scroll.addView(container);
    LinearLayout body = new LinearLayout(activity) {
      @Override protected void onMeasure(int width, int height) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(
            Math.min(View.MeasureSpec.getSize(width), dp(getContext(), 600)),
            View.MeasureSpec.EXACTLY), height);
      }
    };
    body.setOrientation(LinearLayout.VERTICAL);
    container.addView(body, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
    body.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 24));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    body.setTag(root);
    activity.setContentView(root);
    androidx.core.view.ViewCompat.requestApplyInsets(root);
    LinearLayout header = new LinearLayout(activity);
    header.setGravity(Gravity.CENTER_VERTICAL);
    body.addView(header, new LinearLayout.LayoutParams(-1, -2));
    if (back != null) {
      android.widget.ImageButton arrow = new android.widget.ImageButton(activity);
      arrow.setImageResource(android.R.drawable.ic_menu_revert);
      arrow.setColorFilter(INK);
      arrow.setBackgroundResource(android.R.drawable.list_selector_background);
      arrow.setContentDescription("뒤로");
      arrow.setOnClickListener(v -> back.run());
      header.addView(arrow, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
    }
    TextView heading = text(header, title, 24, true);
    heading.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
    return body;
  }

  static TextView text(LinearLayout parent, String value, int size, boolean bold) {
    TextView view = new TextView(parent.getContext());
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(bold ? INK : MUTED);
    view.setLineSpacing(dp(parent.getContext(), 2), 1.15f);
    if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    view.setPadding(0, dp(parent.getContext(), 8), 0, dp(parent.getContext(), 8));
    parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    return view;
  }

  static Button button(LinearLayout parent, String label, int icon, Runnable action) {
    Button button = new AppCompatButton(parent.getContext()) {
      @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
      }
      @Override protected void onMeasure(int width, int height) {
        super.onMeasure(width, height);
        // Compound drawables otherwise hug the button edge instead of the centered label.
        android.graphics.drawable.Drawable drawable = getCompoundDrawablesRelative()[0];
        int content = (int) Math.ceil(getPaint().measureText(getText().toString()));
        if (drawable != null) content += drawable.getBounds().width() + getCompoundDrawablePadding();
        int inset = Math.max(dp(getContext(), 16), (getMeasuredWidth() - content) / 2);
        if (getPaddingLeft() != inset) setPadding(inset, dp(getContext(), 12), inset, dp(getContext(), 12));
        super.onMeasure(width, height);
      }
    };
    button.setText(label);
    button.setAllCaps(false);
    button.setSingleLine(false);
    button.setMaxLines(Integer.MAX_VALUE);
    button.setEllipsize(null);
    button.setTextSize(16);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setGravity(Gravity.CENTER);
    button.setMinHeight(dp(parent.getContext(), 48));
    button.setMinimumWidth(0);
    button.setStateListAnimator(null);
    button.setElevation(0);
    button.setPadding(dp(parent.getContext(), 16), dp(parent.getContext(), 12), dp(parent.getContext(), 16), dp(parent.getContext(), 12));
    if (icon != 0) {
      android.graphics.drawable.Drawable drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(parent.getContext(), icon).mutate();
      drawable.setBounds(0, 0, dp(parent.getContext(), 24), dp(parent.getContext(), 24));
      button.setCompoundDrawablesRelative(drawable, null, null, null);
      button.setCompoundDrawablePadding(dp(parent.getContext(), 8));
    }
    button.setOnClickListener(v -> action.run());
    styleButton(button, false);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(parent.getContext(), 8);
    params.bottomMargin = dp(parent.getContext(), 8);
    parent.addView(button, params);
    return button;
  }

  @android.annotation.SuppressLint("RestrictedApi")
  static void styleButton(Button button, boolean primary) {
    int[][] states = {new int[]{-android.R.attr.state_enabled}, new int[]{}};
    ColorStateList text = new ColorStateList(states, new int[]{Color.rgb(116,129,123), primary ? Color.WHITE : GREEN});
    button.setTextColor(text);
    android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
    background.addState(states[0], shape(button.getContext(), Color.rgb(238,241,239), BORDER, 12));
    background.addState(states[1], shape(button.getContext(), primary ? GREEN : Color.WHITE, primary ? GREEN : BORDER, 12));
    button.setBackgroundTintList(null);
    button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22176D54), background, null));
    // AppCompat 1.7.1 can reapply its separate theme tint after framework tint is cleared.
    if (button instanceof AppCompatButton) ((AppCompatButton) button).setSupportBackgroundTintList(null);
    button.getBackground().setState(button.getDrawableState());
    // Restyling an already enabled view must initialize the new state-list content too.
    background.setState(button.getDrawableState());
    for (android.graphics.drawable.Drawable drawable : button.getCompoundDrawablesRelative()) {
      if (drawable != null) drawable.setTintList(text);
    }
  }

  static void buttonIcon(Button button, int resource) {
    android.graphics.drawable.Drawable icon = androidx.appcompat.content.res.AppCompatResources
        .getDrawable(button.getContext(), resource).mutate();
    int size = dp(button.getContext(), 24);
    icon.setBounds(0, 0, size, size);
    icon.setTintList(button.getTextColors());
    button.setCompoundDrawablesRelative(icon, null, null, null);
    button.requestLayout();
  }

  static TextView timer(LinearLayout parent) {
    FrameLayout container = new FrameLayout(parent.getContext());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(parent.getContext(), 16);
    params.bottomMargin = dp(parent.getContext(), 16);
    parent.addView(container, params);
    TextView timer = new TextView(parent.getContext());
    timer.setTextSize(48);
    timer.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    timer.setTextColor(INK);
    timer.setGravity(Gravity.CENTER);
    timer.setContentDescription("남은 운동 시간");
    timer.setPadding(dp(parent.getContext(), 12), dp(parent.getContext(), 24), dp(parent.getContext(), 12), dp(parent.getContext(), 24));
    if (parent.getResources().getConfiguration().fontScale <= 1.3f) {
      GradientDrawable ring = new GradientDrawable();
      ring.setShape(GradientDrawable.OVAL);
      ring.setColor(BACKGROUND);
      ring.setStroke(dp(parent.getContext(), 8), SOFT);
      timer.setBackground(ring);
      container.addView(timer, new FrameLayout.LayoutParams(dp(parent.getContext(), 224), dp(parent.getContext(), 224), Gravity.CENTER));
    } else container.addView(timer, new FrameLayout.LayoutParams(-1, -2));
    return timer;
  }

  static GradientDrawable shape(Context context, int fill, int border, int radius) {
    GradientDrawable shape = new GradientDrawable();
    shape.setColor(fill);
    shape.setCornerRadius(dp(context, radius));
    shape.setStroke(dp(context, 1), border);
    return shape;
  }

  static LinearLayout card(LinearLayout parent, boolean soft) {
    LinearLayout card = new LinearLayout(parent.getContext());
    card.setOrientation(LinearLayout.VERTICAL);
    int padding = dp(parent.getContext(), 16);
    card.setPadding(padding, padding, padding, padding);
    card.setBackground(shape(parent.getContext(), soft ? SOFT : Color.WHITE, soft ? SOFT : BORDER, 16));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(parent.getContext(), 12);
    params.bottomMargin = dp(parent.getContext(), 8);
    parent.addView(card, params);
    return card;
  }

  static void navigation(LinearLayout body, int selected, Runnable today, Runnable history, Runnable settings) {
    LinearLayout nav = new LinearLayout(body.getContext()) {
      @Override protected void onMeasure(int width, int height) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(
            Math.min(View.MeasureSpec.getSize(width), dp(getContext(), 600)),
            View.MeasureSpec.EXACTLY), height);
      }
    };
    nav.setGravity(Gravity.CENTER);
    nav.setBackgroundColor(Color.WHITE);
    String[] labels = {"오늘", "기록", "설정"};
    int[] icons = {android.R.drawable.ic_menu_today, android.R.drawable.ic_menu_recent_history, android.R.drawable.ic_menu_manage};
    Runnable[] actions = {today, history, settings};
    for (int i = 0; i < labels.length; i++) {
      final int index = i;
      TextView item = new TextView(body.getContext());
      item.setText(labels[i]);
      item.setTextSize(14);
      item.setGravity(Gravity.CENTER);
      item.setTextColor(i == selected ? GREEN : MUTED);
      android.graphics.drawable.Drawable icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(body.getContext(), icons[i]).mutate();
      icon.setTint(i == selected ? GREEN : MUTED);
      icon.setBounds(0, 0, dp(body.getContext(), 24), dp(body.getContext(), 24));
      item.setCompoundDrawables(null, icon, null, null);
      item.setCompoundDrawablePadding(dp(body.getContext(), 4));
      item.setPadding(0, dp(body.getContext(), 12), 0, dp(body.getContext(), 12));
      item.setBackgroundResource(android.R.drawable.list_selector_background);
      item.setOnClickListener(v -> actions[index].run());
      nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }
    FrameLayout bar = new FrameLayout(body.getContext());
    bar.setBackgroundColor(Color.WHITE);
    bar.addView(nav, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
    ((LinearLayout) body.getTag()).addView(bar, new LinearLayout.LayoutParams(-1, -2));
  }

  static RadioGroup options(LinearLayout parent, String[] labels, int selected) {
    RadioGroup group = new RadioGroup(parent.getContext());
    boolean horizontal = parent.getResources().getConfiguration().fontScale <= 1.3f;
    group.setOrientation(horizontal ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);
    for (int i = 0; i < labels.length; i++) {
      RadioButton radio = new RadioButton(parent.getContext());
      radio.setId(View.generateViewId());
      radio.setText(labels[i]);
      radio.setTextColor(INK);
      radio.setTextSize(16);
      radio.setGravity(Gravity.CENTER);
      radio.setButtonDrawable(null);
      radio.setPadding(dp(parent.getContext(), 8), dp(parent.getContext(), 12), dp(parent.getContext(), 8), dp(parent.getContext(), 12));
      radio.setMinHeight(dp(parent.getContext(), 48));
      radio.setButtonTintList(ColorStateList.valueOf(GREEN));
      android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
      background.addState(new int[]{android.R.attr.state_checked}, shape(parent.getContext(), SOFT, GREEN, 12));
      background.addState(new int[]{}, shape(parent.getContext(), Color.WHITE, BORDER, 12));
      radio.setBackground(background);
      RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(horizontal ? 0 : -1, -2, horizontal ? 1 : 0);
      params.setMargins(dp(parent.getContext(), 4), dp(parent.getContext(), 4), dp(parent.getContext(), 4), dp(parent.getContext(), 4));
      group.addView(radio, params);
      if (i == selected) group.check(radio.getId());
    }
    parent.addView(group, new LinearLayout.LayoutParams(-1, -2));
    return group;
  }

  static int selected(RadioGroup group) {
    return group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId()));
  }

  static android.widget.EditText input(LinearLayout parent, String value, String hint, int limit) {
    android.widget.EditText input = new android.widget.EditText(parent.getContext());
    input.setText(value); input.setHint(hint); input.setTextSize(16);
    input.setTextColor(INK); input.setHintTextColor(MUTED);
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(limit)});
    input.setMinHeight(dp(parent.getContext(), 64)); input.setMaxLines(4);
    input.setGravity(Gravity.TOP | Gravity.START);
    input.setPadding(dp(parent.getContext(), 12), dp(parent.getContext(), 12),
        dp(parent.getContext(), 12), dp(parent.getContext(), 12));
    input.setBackground(shape(parent.getContext(), Color.WHITE, BORDER, 12));
    parent.addView(input, new LinearLayout.LayoutParams(-1, -2));
    return input;
  }

  static void week(LinearLayout parent, java.util.Collection<String> completed, String today) {
    java.util.List<String> week = HabitProgress.week(today);
    String[] weekdays = {"월", "화", "수", "목", "금", "토", "일"};
    int columns = parent.getResources().getConfiguration().fontScale > 1.3f ? 4 : 7;
    LinearLayout row = null;
    for (int i = 0; i < 7; i++) {
      if (i % columns == 0) {
        row = new LinearLayout(parent.getContext());
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
      }
      String date = week.get(i);
      boolean done = date.compareTo(today) <= 0 && completed.contains(date);
      boolean current = date.equals(today);
      TextView cell = new TextView(parent.getContext());
      cell.setText(weekdays[i] + "\n" + Integer.parseInt(date.substring(8))
          + "\n" + (done ? "✓" : current ? "오늘" : "·"));
      cell.setContentDescription(date + " " + weekdays[i] + "요일, "
          + (done ? "실천 완료" : date.compareTo(today) > 0 ? "아직 오지 않은 날" : "기록 없음")
          + (current ? ", 오늘" : ""));
      cell.setTextSize(14); cell.setGravity(Gravity.CENTER);
      cell.setTextColor(done ? Color.WHITE : current ? GREEN : MUTED);
      cell.setPadding(0, dp(parent.getContext(), 8), 0, dp(parent.getContext(), 8));
      cell.setLineSpacing(dp(parent.getContext(), 3), 1);
      if (done || current) cell.setBackground(shape(parent.getContext(),
          done ? GREEN : SOFT, GREEN, 12));
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
      params.setMargins(dp(parent.getContext(), 2), dp(parent.getContext(), 4),
          dp(parent.getContext(), 2), dp(parent.getContext(), 4));
      row.addView(cell, params);
    }
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
