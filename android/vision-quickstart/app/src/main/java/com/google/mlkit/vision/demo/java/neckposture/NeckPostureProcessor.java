package com.google.mlkit.vision.demo.java.neckposture;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import java.util.List;
import java.util.Locale;

/** Detects a face ear and matching pose shoulder in the same camera frame. */
public final class NeckPostureProcessor
    extends VisionProcessorBase<NeckPostureProcessor.DetectionResult> {

  private static final String TAG = "NeckPostureProcessor";
  private static final float MIN_SHOULDER_CONFIDENCE = 0.70f;
  // ponytail: yaw is a provisional frontal-face gate, not proof of a true side-on body pose.
  private static final float MIN_SIDE_YAW_DEGREES = 36f;
  public static final int MAX_SHOULDER_OFFSET_PIXELS = 80;

  private final FaceDetector faceDetector;
  private final PoseDetector poseDetector;
  private long lastLogMs;
  private int shoulderOffsetPixels;

  public void setShoulderOffsetPixels(int pixels) {
    if (pixels < 0 || pixels > MAX_SHOULDER_OFFSET_PIXELS) {
      throw new IllegalArgumentException("Shoulder offset outside adjustment range");
    }
    shoulderOffsetPixels = pixels;
  }

  public NeckPostureProcessor(Context context) {
    super(context);
    FaceDetectorOptions faceOptions =
        new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build();
    faceDetector = FaceDetection.getClient(faceOptions);
    poseDetector =
        PoseDetection.getClient(
            new AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build());
  }

  @Override
  public void stop() {
    super.stop();
    faceDetector.close();
    poseDetector.close();
  }

  @Override
  protected Task<DetectionResult> detectInImage(InputImage image) {
    Task<List<Face>> faceTask = faceDetector.process(image);
    Task<Pose> poseTask = poseDetector.process(image);
    return Tasks.whenAll(faceTask, poseTask)
        .continueWith(
            task -> {
              task.getResult();
              return new DetectionResult(faceTask.getResult(), poseTask.getResult());
            });
  }

  @Override
  protected void onSuccess(
      @NonNull DetectionResult result, @NonNull GraphicOverlay graphicOverlay) {
    Measurement measurement = measure(result.faces, result.pose,
        graphicOverlay.getImageWidth(), graphicOverlay.getImageHeight());
    measurement = adjustShoulder(measurement, shoulderOffsetPixels,
        graphicOverlay.getImageWidth(), graphicOverlay.getImageHeight());
    graphicOverlay.add(new NeckPostureGraphic(graphicOverlay, result, measurement));
    logMeasurement(result, measurement);
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Neck posture detection failed", e);
  }

  private static Measurement measure(List<Face> faces, Pose pose, int width, int height) {
    if (faces.size() > 1) {
      return Measurement.invalid("화면에 한 사람만 들어오게 해 주세요");
    }
    Face face = largestFace(faces);
    if (face == null) {
      return Measurement.invalid("얼굴을 카메라 쪽으로 조금 돌려 주세요");
    }

    FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
    FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
    if (leftEar == null && rightEar == null) {
      return Measurement.invalid("귀가 보이도록 옆으로 서 주세요");
    }
    int side = sideForYaw(face.getHeadEulerAngleY());
    if (side == 0) {
      return Measurement.invalid("정면이 아닌 옆모습을 보여 주세요");
    }

    boolean useLeftSide = side < 0;
    FaceLandmark selectedEar = face.getLandmark(faceEarTypeForPoseSide(side));
    if (selectedEar == null) {
      return Measurement.invalid("카메라 쪽 귀가 검출되지 않았습니다");
    }
    PointF ear = selectedEar.getPosition();
    PoseLandmark shoulder =
        pose.getPoseLandmark(
            useLeftSide ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER);
    if (shoulder == null || !(shoulder.getInFrameLikelihood() >= MIN_SHOULDER_CONFIDENCE)) {
      return Measurement.invalid("같은 쪽 어깨 검출이 불안정합니다");
    }

    PointF shoulderPosition = shoulder.getPosition();
    if (!NeckPostureAngle.isInsideImage(ear, width, height)
        || !NeckPostureAngle.isInsideImage(shoulderPosition, width, height)) {
      return Measurement.invalid("귀와 어깨가 모두 화면 안에 들어오게 해 주세요");
    }
    double angle = NeckPostureAngle.calculate(ear, shoulderPosition);
    if (Double.isNaN(angle)) {
      return Measurement.invalid("귀와 어깨 위치를 다시 맞춰 주세요");
    }
    return Measurement.valid(ear, shoulderPosition, angle, useLeftSide ? -1 : 1);
  }

  static int sideForYaw(float yaw) {
    if (Float.isNaN(yaw) || Float.isInfinite(yaw) || Math.abs(yaw) > 180f
        || Math.abs(yaw) < MIN_SIDE_YAW_DEGREES) {
      return 0;
    }
    return yaw < 0 ? -1 : 1;
  }

  static Measurement adjustShoulder(Measurement raw, int pixels, int width, int height) {
    if (!raw.isValid() || pixels == 0) {
      return raw;
    }
    PointF adjusted = new PointF(raw.shoulder.x, raw.shoulder.y + pixels);
    if (pixels < 0 || pixels > MAX_SHOULDER_OFFSET_PIXELS
        || !NeckPostureAngle.isInsideImage(adjusted, width, height)) {
      return Measurement.invalid("수동 보정점이 화면 밖입니다. 하강량을 줄여 주세요");
    }
    return new Measurement(raw.ear, adjusted, raw.shoulder,
        NeckPostureAngle.calculate(raw.ear, adjusted), "수동 보정", raw.side);
  }

  static int faceEarTypeForPoseSide(int side) {
    // Pinned by mirrored photo replay on face-detection 16.1.7: Face/Pose ear-side names differ.
    if (side == -1) {
      return FaceLandmark.RIGHT_EAR;
    }
    if (side == 1) {
      return FaceLandmark.LEFT_EAR;
    }
    throw new IllegalArgumentException("A left or right pose side is required");
  }

  static Face largestFace(List<Face> faces) {
    Face largest = null;
    int largestArea = 0;
    for (Face face : faces) {
      int area = face.getBoundingBox().width() * face.getBoundingBox().height();
      if (area > largestArea) {
        largest = face;
        largestArea = area;
      }
    }
    return largest;
  }

  private void logMeasurement(DetectionResult result, Measurement measurement) {
    long nowMs = System.currentTimeMillis();
    if (nowMs - lastLogMs < 1000) {
      return;
    }
    lastLogMs = nowMs;
    Face face = largestFace(result.faces);
    if (face != null) {
      Log.i(TAG, String.format(Locale.US, "face yaw=%.1f, leftEar=%s, rightEar=%s",
          face.getHeadEulerAngleY(), face.getLandmark(FaceLandmark.LEFT_EAR) != null,
          face.getLandmark(FaceLandmark.RIGHT_EAR) != null));
      logFaceLandmark("LEFT_EAR", face.getLandmark(FaceLandmark.LEFT_EAR));
      logFaceLandmark("RIGHT_EAR", face.getLandmark(FaceLandmark.RIGHT_EAR));
      logFaceLandmark("NOSE_BASE", face.getLandmark(FaceLandmark.NOSE_BASE));
    }
    logShoulder("LEFT", result.pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER));
    logShoulder("RIGHT", result.pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER));
    if (!measurement.isValid()) {
      Log.i(TAG, measurement.message);
      return;
    }
    Log.i(
        TAG,
        String.format(
            Locale.US,
            "ear=(%.1f, %.1f), shoulder=(%.1f, %.1f), angle=%.2f, rawAngle=%.2f, offsetY=%.1f, belowReference=%s",
            measurement.ear.x,
            measurement.ear.y,
            measurement.shoulder.x,
            measurement.shoulder.y,
            measurement.angle,
            measurement.rawAngle,
            measurement.shoulder.y - measurement.rawShoulder.y,
            measurement.isAdjusted() ? "excluded-manual" :
                Boolean.toString(NeckPostureAngle.isBelowReference(measurement.angle))));
  }

  private static void logFaceLandmark(String name, FaceLandmark landmark) {
    Log.i(TAG, landmark == null ? name + ": missing" : String.format(Locale.US,
        "%s raw=(%.1f, %.1f)", name, landmark.getPosition().x, landmark.getPosition().y));
  }

  private static void logShoulder(String side, PoseLandmark shoulder) {
    if (shoulder == null) {
      Log.i(TAG, side + " shoulder: missing");
      return;
    }
    Log.i(TAG, String.format(Locale.US, "%s shoulder raw=(%.1f, %.1f), confidence=%.3f",
        side, shoulder.getPosition().x, shoulder.getPosition().y,
        shoulder.getInFrameLikelihood()));
  }

  static final class DetectionResult {
    final List<Face> faces;
    final Pose pose;

    DetectionResult(List<Face> faces, Pose pose) {
      this.faces = faces;
      this.pose = pose;
    }
  }

  static final class Measurement {
    final PointF ear;
    final PointF shoulder;
    final PointF rawShoulder;
    final double rawAngle;
    final double angle;
    final String message;
    final int side;

    private Measurement(PointF ear, PointF shoulder, PointF rawShoulder,
        double angle, String message, int side) {
      this.ear = ear;
      this.shoulder = shoulder;
      this.rawShoulder = rawShoulder;
      this.rawAngle = rawShoulder == null ? Double.NaN : NeckPostureAngle.calculate(ear, rawShoulder);
      this.angle = angle;
      this.message = message;
      this.side = side;
    }

    static Measurement valid(PointF ear, PointF shoulder, double angle, int side) {
      return new Measurement(ear, shoulder, shoulder, angle, "측정 중", side);
    }

    static Measurement invalid(String message) {
      return new Measurement(null, null, null, Double.NaN, message, 0);
    }

    boolean isAdjusted() {
      return isValid() && shoulder.y != rawShoulder.y;
    }

    boolean isValid() {
      return ear != null && shoulder != null && angle > 0 && angle <= 90;
    }
  }
}
