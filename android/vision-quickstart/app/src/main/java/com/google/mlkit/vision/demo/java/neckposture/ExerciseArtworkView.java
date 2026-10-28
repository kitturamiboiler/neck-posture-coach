package com.google.mlkit.vision.demo.java.neckposture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.view.View;
import java.io.InputStream;

/** Original instructional art; no third-party video is downloaded or republished. */
final class ExerciseArtworkView extends View {
  private Bitmap art;
  private int exercise;
  private boolean playing;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

  ExerciseArtworkView(Context context, int exercise) {
    super(context);
    this.exercise = exercise;
    setBackgroundColor(android.graphics.Color.WHITE);
    try (InputStream input = context.getAssets().open("exercise_guide.png")) {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = 2;
      art = BitmapFactory.decodeStream(input, null, options);
    } catch (java.io.IOException ignored) { }
    setContentDescription(ExerciseLibrary.TITLES[exercise] + " 동작 참고 그림");
  }

  void exercise(int value) {
    if (exercise != value) { exercise = value; setContentDescription(ExerciseLibrary.TITLES[value] + " 동작 참고 그림"); invalidate(); }
  }
  void playing(boolean value) { playing = value; invalidate(); }

  @Override protected void onMeasure(int width, int height) {
    int w = MeasureSpec.getSize(width);
    setMeasuredDimension(w, Math.max(CoachUi.dp(getContext(), 160), Math.min(CoachUi.dp(getContext(), 240), w / 2)));
  }
  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (art == null) {
      paint.setColor(CoachUi.MUTED); paint.setTextSize(CoachUi.dp(getContext(), 16));
      canvas.drawText("동작 설명을 확인해 주세요", CoachUi.dp(getContext(), 16), getHeight() / 2f, paint);
      return;
    }
    int top = exercise * art.getHeight() / 2, bottom = (exercise + 1) * art.getHeight() / 2;
    Rect source = new Rect(0, top, art.getWidth(), bottom);
    if (playing) {
      boolean second = (android.os.SystemClock.uptimeMillis() / 1800) % 2 == 1;
      source.left = second ? art.getWidth() * 52 / 100 : 0;
      source.right = second ? art.getWidth() : art.getWidth() * 48 / 100;
      postInvalidateDelayed(200);
    }
    float scale = Math.min(getWidth() / (float) source.width(), getHeight() / (float) source.height());
    float w = source.width() * scale, h = source.height() * scale;
    canvas.drawBitmap(art, source, new RectF((getWidth()-w)/2, (getHeight()-h)/2, (getWidth()+w)/2, (getHeight()+h)/2), paint);
  }
}
