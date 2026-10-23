package com.google.mlkit.vision.demo.java.neckposture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Uses the FIT_CENTER preview's image rectangle, excluding letterboxing. */
final class CaptureGuideView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int imageWidth = 3, imageHeight = 4;
  private boolean ready;

  CaptureGuideView(Context context) {
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  void update(int width, int height, boolean aligned) {
    imageWidth = width; imageHeight = height; ready = aligned; invalidate();
  }

  static RectF imageRect(int viewWidth, int viewHeight, int imageWidth, int imageHeight) {
    float scale = Math.min(viewWidth / (float) imageWidth, viewHeight / (float) imageHeight);
    float w = imageWidth * scale, h = imageHeight * scale;
    float x = (viewWidth - w) / 2, y = (viewHeight - h) / 2;
    return new RectF(x, y, x + w, y + h);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    RectF image = imageRect(getWidth(), getHeight(), imageWidth, imageHeight);
    float w = image.width(), h = image.height(), x = image.left, y = image.top;
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(CoachUi.dp(getContext(), 2));
    paint.setColor(ready ? 0xff69f0ae : 0xffffffff);
    int radius = CoachUi.dp(getContext(), 12);
    canvas.drawRoundRect(new RectF(x + w * CaptureReadiness.LEFT, y + h * CaptureReadiness.TOP,
        x + w * CaptureReadiness.RIGHT, y + h * CaptureReadiness.BOTTOM), radius, radius, paint);
    paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{12, 8}, 0));
    canvas.drawRoundRect(new RectF(x + w * .05f, y + h * .58f, x + w * .95f, y + h * .97f), radius, radius, paint);
    paint.setPathEffect(null);
    canvas.drawLine(x + w / 2 - radius, y + h * .58f, x + w / 2 + radius, y + h * .58f, paint);
  }
}
