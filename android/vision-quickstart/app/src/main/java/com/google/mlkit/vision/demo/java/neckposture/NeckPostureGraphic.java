package com.google.mlkit.vision.demo.java.neckposture;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.PoseLandmark;
import java.util.Locale;

/** Draws the selected ear, shoulder, horizontal reference, and measured angle. */
final class NeckPostureGraphic extends Graphic {

  private static final float POINT_RADIUS = 14f;
  private static final float STROKE_WIDTH = 8f;
  private static final float TEXT_SIZE = 42f;

  private final NeckPostureProcessor.DetectionResult result;
  private final NeckPostureProcessor.Measurement measurement;
  private final Paint linePaint = paint(Color.WHITE, Paint.Style.STROKE, STROKE_WIDTH);
  private final Paint referencePaint = paint(Color.CYAN, Paint.Style.STROKE, STROKE_WIDTH / 2f);
  private final Paint verticalPaint = paint(Color.YELLOW, Paint.Style.STROKE, STROKE_WIDTH / 2f);
  private final Paint pointPaint = paint(Color.WHITE, Paint.Style.FILL, 0);
  private final Paint textPaint = paint(Color.WHITE, Paint.Style.FILL, 0);
  private final Paint anglePaint = paint(Color.CYAN, Paint.Style.STROKE, STROKE_WIDTH);
  private final Paint footerPaint = paint(0xCC000000, Paint.Style.FILL, 0);
  private final Paint nosePaint = paint(Color.MAGENTA, Paint.Style.STROKE, 4f);
  private final Paint rawPaint = paint(Color.LTGRAY, Paint.Style.STROKE, 3f);

  NeckPostureGraphic(
      GraphicOverlay overlay,
      NeckPostureProcessor.DetectionResult result,
      NeckPostureProcessor.Measurement measurement) {
    super(overlay);
    this.result = result;
    this.measurement = measurement;
    textPaint.setTextSize(TEXT_SIZE);
    textPaint.setShadowLayer(6f, 0f, 0f, Color.BLACK);
  }

  @Override
  public void draw(Canvas canvas) {
    drawNoseReference(canvas);
    if (!measurement.isValid()) {
      drawDetectedLandmarks(canvas);
      drawFooter(canvas, "측정 대기", measurement.message);
      return;
    }

    float earX = translateX(measurement.ear.x);
    float earY = translateY(measurement.ear.y);
    float shoulderX = translateX(measurement.shoulder.x);
    float shoulderY = translateY(measurement.shoulder.y);

    if (measurement.isAdjusted()) {
      float rawY = translateY(measurement.rawShoulder.y);
      canvas.drawLine(shoulderX, rawY, earX, earY, rawPaint);
      canvas.drawCircle(shoulderX, rawY, POINT_RADIUS, rawPaint);
      drawLabel(canvas, "원시 어깨", shoulderX, rawY);
    }

    canvas.drawLine(0, shoulderY, canvas.getWidth(), shoulderY, referencePaint);
    canvas.drawLine(shoulderX, 0, shoulderX, canvas.getHeight(), verticalPaint);
    canvas.drawLine(shoulderX, shoulderY, earX, earY, linePaint);
    boolean belowReference = NeckPostureAngle.isBelowReference(measurement.angle);
    anglePaint.setColor(measurement.isAdjusted() ? Color.LTGRAY :
        (belowReference ? Color.rgb(255, 138, 101) : Color.CYAN));
    float radius = Math.min(100f, (float) Math.hypot(earX - shoulderX, earY - shoulderY) * 0.25f);
    boolean earOnRight = earX >= shoulderX;
    canvas.drawArc(shoulderX - radius, shoulderY - radius,
        shoulderX + radius, shoulderY + radius, earOnRight ? 0f : 180f,
        (float) measurement.angle * (earOnRight ? -1f : 1f), false, anglePaint);
    canvas.drawCircle(earX, earY, POINT_RADIUS, pointPaint);
    canvas.drawCircle(shoulderX, shoulderY, POINT_RADIUS, pointPaint);
    String side = measurement.side < 0 ? "L " : "R ";
    drawLabel(canvas, side + "귀 원시", earX, earY);
    drawLabel(canvas, side + (measurement.isAdjusted() ? "수동 어깨" : "어깨"),
        shoulderX, shoulderY + TEXT_SIZE * 1.5f);
    if (measurement.isAdjusted()) {
      drawFooter(canvas, String.format(Locale.US, "원시 %.2f° · 수동 %.2f°",
          measurement.rawAngle, measurement.angle), "수동 보정값 · 50° 비교 제외");
      return;
    }
    drawFooter(canvas, String.format(Locale.US, "귀-어깨각 %.2f° · 참고용", measurement.angle),
        String.format(Locale.US, "%.0f° %s · 진단 아님",
            NeckPostureAngle.REFERENCE_ANGLE_DEGREES, belowReference ? "미만" : "이상"));
  }

  private void drawNoseReference(Canvas canvas) {
    Face face = result == null ? null : NeckPostureProcessor.largestFace(result.faces);
    FaceLandmark nose = face == null ? null : face.getLandmark(FaceLandmark.NOSE_BASE);
    if (nose == null) {
      return;
    }
    float x = translateX(nose.getPosition().x);
    float y = translateY(nose.getPosition().y);
    canvas.drawLine(x - POINT_RADIUS, y, x + POINT_RADIUS, y, nosePaint);
    canvas.drawLine(x, y - POINT_RADIUS, x, y + POINT_RADIUS, nosePaint);
    textPaint.setColor(Color.MAGENTA);
    drawLabel(canvas, "코 원시", x, y + TEXT_SIZE * 1.5f);
    textPaint.setColor(Color.WHITE);
  }

  private void drawDetectedLandmarks(Canvas canvas) {
    Face face = NeckPostureProcessor.largestFace(result.faces);
    if (face != null) {
      drawFaceLandmark(canvas, face.getLandmark(NeckPostureProcessor.faceEarTypeForPoseSide(-1)), "L 귀 후보");
      drawFaceLandmark(canvas, face.getLandmark(NeckPostureProcessor.faceEarTypeForPoseSide(1)), "R 귀 후보");
    }
    drawPoseLandmark(
        canvas, result.pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER), "L 어깨 후보");
    drawPoseLandmark(
        canvas, result.pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER), "R 어깨 후보");
  }

  private void drawFaceLandmark(Canvas canvas, FaceLandmark landmark, String label) {
    if (landmark == null) {
      return;
    }
    drawCandidate(canvas, landmark.getPosition().x, landmark.getPosition().y, label);
  }

  private void drawPoseLandmark(Canvas canvas, PoseLandmark landmark, String label) {
    if (landmark == null) {
      return;
    }
    drawCandidate(
        canvas,
        landmark.getPosition().x,
        landmark.getPosition().y,
        String.format(Locale.US, "%s %.2f", label, landmark.getInFrameLikelihood()));
  }

  private void drawCandidate(Canvas canvas, float x, float y, String label) {
    float screenX = translateX(x);
    float screenY = translateY(y);
    canvas.drawCircle(screenX, screenY, POINT_RADIUS, pointPaint);
    drawLabel(canvas, label, screenX, screenY);
  }

  private void drawLabel(Canvas canvas, String label, float x, float y) {
    float labelX = Math.max(0, Math.min(x + POINT_RADIUS,
        canvas.getWidth() - textPaint.measureText(label)));
    float labelY = Math.max(TEXT_SIZE, Math.min(y - POINT_RADIUS, canvas.getHeight() - 8f));
    canvas.drawText(label, labelX, labelY, textPaint);
  }

  private void drawFooter(Canvas canvas, String firstLine, String secondLine) {
    float top = canvas.getHeight() - TEXT_SIZE * 2.8f;
    canvas.drawRect(0, top, canvas.getWidth(), canvas.getHeight(), footerPaint);
    drawCenteredText(canvas, firstLine, top + TEXT_SIZE * 1.1f);
    drawCenteredText(canvas, secondLine, top + TEXT_SIZE * 2.3f);
  }

  private void drawCenteredText(Canvas canvas, String text, float y) {
    float availableWidth = Math.max(1f, canvas.getWidth() - 24f);
    textPaint.setTextSize(TEXT_SIZE * Math.min(1f, availableWidth / textPaint.measureText(text)));
    canvas.drawText(text, (canvas.getWidth() - textPaint.measureText(text)) / 2f, y, textPaint);
    textPaint.setTextSize(TEXT_SIZE);
  }

  private static Paint paint(int color, Paint.Style style, float strokeWidth) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(color);
    paint.setStyle(style);
    paint.setStrokeWidth(strokeWidth);
    return paint;
  }
}
