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
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import java.util.List;
import java.util.Locale;

/** Detects a face ear and matching pose shoulder in the same camera frame. */
public final class NeckPostureProcessor
    extends VisionProcessorBase<NeckPostureProcessor.DetectionResult> {

  private static final String TAG = "NeckPostureProcessor";
  private static final float MIN_SHOULDER_CONFIDENCE = 0.70f;
  private static final float SMOOTHING_ALPHA = 0.20f;

  private final FaceDetector faceDetector;
  private final PoseDetector poseDetector;
  private long lastLogMs;
  private PointF smoothedEar;
  private PointF smoothedShoulder;
  private int smoothedSide;

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
        PoseDetection.getClient(PreferenceUtils.getPoseDetectorOptionsForLivePreview(context));
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
    Measurement measurement = smooth(measure(result.faces, result.pose));
    graphicOverlay.add(new NeckPostureGraphic(graphicOverlay, result, measurement));
    logMeasurement(measurement);
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Neck posture detection failed", e);
  }

  private static Measurement measure(List<Face> faces, Pose pose) {
    Face face = largestFace(faces);
    if (face == null) {
      return Measurement.invalid("얼굴을 카메라 쪽으로 조금 돌려 주세요");
    }

    FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
    FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
    if (leftEar == null && rightEar == null) {
      return Measurement.invalid("귀가 보이도록 옆으로 서 주세요");
    }
    if (leftEar != null && rightEar != null) {
      return Measurement.invalid("정면이 아닌 옆모습을 보여 주세요");
    }

    boolean useLeftSide = leftEar != null;
    PointF ear = (useLeftSide ? leftEar : rightEar).getPosition();
    PoseLandmark shoulder =
        pose.getPoseLandmark(
            useLeftSide ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER);
    if (shoulder == null || shoulder.getInFrameLikelihood() < MIN_SHOULDER_CONFIDENCE) {
      return Measurement.invalid("어깨가 화면에 들어오도록 조금 뒤로 서 주세요");
    }

    PointF shoulderPosition = shoulder.getPosition();
    double angle = NeckPostureAngle.calculate(ear, shoulderPosition);
    if (Double.isNaN(angle)) {
      return Measurement.invalid("귀와 어깨 위치를 다시 맞춰 주세요");
    }
    return Measurement.valid(ear, shoulderPosition, angle, useLeftSide ? -1 : 1);
  }

  private Measurement smooth(Measurement measurement) {
    if (!measurement.isValid()) {
      return measurement;
    }
    if (smoothedEar == null || smoothedSide != measurement.side) {
      smoothedEar = new PointF(measurement.ear.x, measurement.ear.y);
      smoothedShoulder = new PointF(measurement.shoulder.x, measurement.shoulder.y);
      smoothedSide = measurement.side;
    } else {
      smoothedEar = interpolate(smoothedEar, measurement.ear);
      smoothedShoulder = interpolate(smoothedShoulder, measurement.shoulder);
    }
    return Measurement.valid(
        smoothedEar,
        smoothedShoulder,
        NeckPostureAngle.calculate(smoothedEar, smoothedShoulder),
        smoothedSide);
  }

  private static PointF interpolate(PointF previous, PointF current) {
    return new PointF(
        previous.x + SMOOTHING_ALPHA * (current.x - previous.x),
        previous.y + SMOOTHING_ALPHA * (current.y - previous.y));
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

  private void logMeasurement(Measurement measurement) {
    long nowMs = System.currentTimeMillis();
    if (nowMs - lastLogMs < 1000) {
      return;
    }
    lastLogMs = nowMs;
    if (!measurement.isValid()) {
      Log.i(TAG, measurement.message);
      return;
    }
    Log.i(
        TAG,
        String.format(
            Locale.US,
            "Ear=(%.1f, %.1f), shoulder=(%.1f, %.1f), angle=%.1f",
            measurement.ear.x,
            measurement.ear.y,
            measurement.shoulder.x,
            measurement.shoulder.y,
            measurement.angle));
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
    final double angle;
    final String message;
    final int side;

    private Measurement(PointF ear, PointF shoulder, double angle, String message, int side) {
      this.ear = ear;
      this.shoulder = shoulder;
      this.angle = angle;
      this.message = message;
      this.side = side;
    }

    static Measurement valid(PointF ear, PointF shoulder, double angle, int side) {
      return new Measurement(ear, shoulder, angle, "측정 중", side);
    }

    static Measurement invalid(String message) {
      return new Measurement(null, null, Double.NaN, message, 0);
    }

    boolean isValid() {
      return ear != null && shoulder != null && !Double.isNaN(angle);
    }
  }
}
