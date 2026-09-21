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
  private final Paint pointPaint = paint(Color.WHITE, Paint.Style.FILL, 0);
  private final Paint textPaint = paint(Color.WHITE, Paint.Style.FILL, 0);

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
    if (!measurement.isValid()) {
      drawDetectedLandmarks(canvas);
    }
    drawCenteredText(canvas, measurement.message, TEXT_SIZE * 1.5f);
    if (!measurement.isValid()) {
      return;
    }

    float earX = translateX(measurement.ear.x);
    float earY = translateY(measurement.ear.y);
    float shoulderX = translateX(measurement.shoulder.x);
    float shoulderY = translateY(measurement.shoulder.y);

    canvas.drawLine(shoulderX, shoulderY, earX, shoulderY, referencePaint);
    canvas.drawLine(shoulderX, shoulderY, earX, earY, linePaint);
    canvas.drawCircle(earX, earY, POINT_RADIUS, pointPaint);
    canvas.drawCircle(shoulderX, shoulderY, POINT_RADIUS, pointPaint);
    canvas.drawText("귀", earX + POINT_RADIUS, earY, textPaint);
    canvas.drawText("어깨", shoulderX + POINT_RADIUS, shoulderY, textPaint);
    drawCenteredText(
        canvas,
        String.format(Locale.US, "귀-어깨각 %.1f°", measurement.angle),
        TEXT_SIZE * 2.8f);
  }

  private void drawDetectedLandmarks(Canvas canvas) {
    Face face = NeckPostureProcessor.largestFace(result.faces);
    if (face != null) {
      drawFaceLandmark(canvas, face.getLandmark(FaceLandmark.LEFT_EAR), "귀 후보");
      drawFaceLandmark(canvas, face.getLandmark(FaceLandmark.RIGHT_EAR), "귀 후보");
    }
    drawPoseLandmark(
        canvas, result.pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER), "어깨 후보");
    drawPoseLandmark(
        canvas, result.pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER), "어깨 후보");
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
    canvas.drawText(label, screenX + POINT_RADIUS, screenY, textPaint);
  }

  private void drawCenteredText(Canvas canvas, String text, float y) {
    canvas.drawText(text, (canvas.getWidth() - textPaint.measureText(text)) / 2f, y, textPaint);
  }

  private static Paint paint(int color, Paint.Style style, float strokeWidth) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(color);
    paint.setStyle(style);
    paint.setStrokeWidth(strokeWidth);
    return paint;
  }
}
