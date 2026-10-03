package com.google.mlkit.vision.demo.java.neckposture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatImageView;

/** ImageView's own matrix is shared by drawing and inverse touch mapping. */
final class PosturePhotoView extends AppCompatImageView {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private Bitmap bitmap;
  PointF ear, base;
  boolean editingEar = true;
  Runnable changed = () -> {};

  PosturePhotoView(Context context) {
    super(context);
    setScaleType(ScaleType.FIT_CENTER);
    setBackgroundColor(Color.rgb(21, 26, 28));
    setContentDescription("옆모습 사진의 귀와 C7 기준점");
    setFocusable(true);
  }

  void photo(Bitmap value, PointF ear, PointF base) {
    // Drawable density scaling would make image coordinates disagree with the touch matrix.
    value.setDensity(Bitmap.DENSITY_NONE);
    bitmap = value;
    this.ear = ear == null ? null : new PointF(ear.x, ear.y);
    this.base = base == null ? null : new PointF(base.x, base.y);
    setImageBitmap(value);
  }

  double angle() {
    return ear == null || base == null ? Double.NaN
        : CvaMath.angle(ear.x, ear.y, base.x, base.y);
  }

  void nudge(float dx, float dy) {
    PointF point = editingEar ? ear : base;
    if (bitmap == null) return;
    if (point == null) point = new PointF(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
    place(new PointF(Math.max(0, Math.min(bitmap.getWidth() - 1, point.x + dx)),
        Math.max(0, Math.min(bitmap.getHeight() - 1, point.y + dy))));
  }

  private void place(PointF point) {
    if (editingEar) ear = point; else base = point;
    changed.run();
    invalidate();
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    if (bitmap == null) return false;
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN
        || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
      Matrix inverse = new Matrix();
      if (!getImageMatrix().invert(inverse)) return false;
      float[] point = {event.getX() - getPaddingLeft(), event.getY() - getPaddingTop()};
      inverse.mapPoints(point);
      if (point[0] >= 0 && point[0] < bitmap.getWidth()
          && point[1] >= 0 && point[1] < bitmap.getHeight()) {
        getParent().requestDisallowInterceptTouchEvent(true);
        place(new PointF(point[0], point[1]));
      }
      return true;
    }
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      getParent().requestDisallowInterceptTouchEvent(false);
      performClick();
      return true;
    }
    if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      getParent().requestDisallowInterceptTouchEvent(false);
      return true;
    }
    return super.onTouchEvent(event);
  }

  @Override public boolean performClick() { super.performClick(); return true; }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (bitmap == null) return;
    PointF e = mapped(ear), b = mapped(base);
    paint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
    if (e != null && b != null) {
      paint.setColor(Color.rgb(80, 221, 205));
      canvas.drawLine(0, b.y, getWidth(), b.y, paint);
      paint.setColor(Color.rgb(250, 211, 83));
      canvas.drawLine(b.x, 0, b.x, getHeight(), paint);
      paint.setColor(Color.WHITE);
      canvas.drawLine(b.x, b.y, e.x, e.y, paint);
      double angle = angle();
      if (!Double.isNaN(angle)) {
        float radius = Math.min(40 * getResources().getDisplayMetrics().density,
            (float) Math.hypot(e.x - b.x, e.y - b.y) / 3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(80, 221, 205));
        canvas.drawArc(new RectF(b.x - radius, b.y - radius, b.x + radius, b.y + radius),
            e.x >= b.x ? -(float) angle : 180, (float) angle, false, paint);
        paint.setStyle(Paint.Style.FILL);
      }
    }
    dot(canvas, e, "귀");
    dot(canvas, b, "C7");
  }

  private PointF mapped(PointF source) {
    if (source == null) return null;
    float[] point = {source.x, source.y};
    getImageMatrix().mapPoints(point);
    return new PointF(point[0] + getPaddingLeft(), point[1] + getPaddingTop());
  }

  private void dot(Canvas canvas, PointF point, String label) {
    if (point == null) return;
    float density = getResources().getDisplayMetrics().density;
    paint.setColor(Color.WHITE);
    canvas.drawCircle(point.x, point.y, 6 * density, paint);
    paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
    paint.setShadowLayer(3 * density, 0, 0, Color.BLACK);
    float x = Math.max(4 * density, Math.min(point.x + 10 * density,
        getWidth() - paint.measureText(label) - 4 * density));
    canvas.drawText(label, x, Math.max(20 * density, point.y - 10 * density), paint);
    paint.clearShadowLayer();
  }
}
