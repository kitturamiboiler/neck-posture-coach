package com.google.mlkit.vision.demo.java.neckposture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PostureCaptureActivity extends AppCompatActivity implements SensorEventListener {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private PostureSession state;
  private PostureStore store;
  private FaceDetector detector;
  private ProcessCameraProvider provider;
  private ImageCapture capture;
  private PreviewView preview;
  private TextView cover, status;
  private Button shutter;
  private boolean active, shooting;
  private volatile int generation;
  private SensorManager sensors;
  private Sensor gravity;
  private boolean level;
  private final Random random = new Random();

  @Override protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    getWindow().setStatusBarColor(CoachUi.GREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    state = new ViewModelProvider(this).get(PostureSession.class);
    store = new PostureStore(this);
    detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setMinFaceSize(0.1f).build());
    sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
    gravity = sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
    if (gravity == null) gravity = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    level = gravity == null;
  }

  @Override protected void onResume() {
    super.onResume();
    active = true;
    if (!store.hasPlan()) { finish(); return; }
    if (gravity != null) sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
    if (state.screen.equals("result") && state.photos.size() == 3) result();
    else if (state.photos.size() == 3) annotate();
    else if (state.screen.equals("camera")) camera();
    else preparation();
  }

  private void preparation() {
    stopCamera();
    state.clearPhotos();
    state.screen = "prepare";
    LinearLayout body = CoachUi.page(this, "사진 점검");
    CoachUi.text(body, "같은 조건으로, 자연스러운 옆모습", 20, true);
    CoachUi.text(body, "서 있는 자세 · 어깨 높이의 카메라 · 수평인 기기\n귀와 C7 표식이 함께 보이는 정확한 옆면", 16, false);
    CoachUi.text(body, "C7 위치는 먼저 전문가 또는 위치를 확인할 수 있는 도움을 받아 표식으로 남겨 주세요. 위치가 확실하지 않으면 이번 측정을 보류하세요.", 15, false);
    CoachUi.text(body, "촬영하는 쪽", 16, true);
    RadioGroup side = CoachUi.options(body, new String[]{"왼쪽 옆모습", "오른쪽 옆모습"},
        state.side.equals("왼쪽") ? 0 : 1);
    CoachUi.text(body, "카메라", 16, true);
    RadioGroup camera = CoachUi.options(body, new String[]{"후면 카메라", "전면 카메라"},
        state.camera.equals("후면") ? 0 : 1);
    CheckBox protocol = new CheckBox(this);
    protocol.setText("C7 위치와 촬영 조건을 확인했어요");
    protocol.setTextColor(CoachUi.INK);
    protocol.setMinHeight(CoachUi.dp(this, 48));
    body.addView(protocol);
    CoachUi.text(body, "촬영 중에는 미리보기가 가려지고 여러 장이 촬영됩니다. 턱을 일부러 당기거나 목을 앞으로 내밀지 말고 평소처럼 시선을 두세요.", 15, false);
    Button next = CoachUi.button(body, "카메라 열기", android.R.drawable.ic_menu_camera, () -> {
      state.side = CoachUi.selected(side) == 0 ? "왼쪽" : "오른쪽";
      state.camera = CoachUi.selected(camera) == 0 ? "후면" : "전면";
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 7);
      } else camera();
    });
    next.setEnabled(false);
    protocol.setOnCheckedChangeListener((button, checked) -> next.setEnabled(checked));
    CoachUi.button(body, "오늘로 돌아가기", android.R.drawable.ic_menu_revert, this::finish);
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grants) {
    super.onRequestPermissionsResult(requestCode, permissions, grants);
    if (requestCode == 7) {
      if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
        state.screen = "camera";
        if (active) camera();
      } else error("카메라 권한이 필요합니다. 설정에서 권한을 허용한 뒤 다시 시도해 주세요.");
    }
  }

  private void camera() {
    stopCamera();
    state.clearPhotos();
    state.screen = "camera";
    final int token = generation;
    LinearLayout body = CoachUi.page(this, "자연스러운 옆모습");
    FrameLayout frame = new FrameLayout(this);
    int height = Math.max(220, Math.min(420, getResources().getConfiguration().screenHeightDp / 2));
    body.addView(frame, new LinearLayout.LayoutParams(-1, CoachUi.dp(this, height)));
    preview = new PreviewView(this);
    preview.setScaleType(PreviewView.ScaleType.FIT_CENTER);
    frame.addView(preview, new FrameLayout.LayoutParams(-1, -1));
    cover = new TextView(this);
    cover.setGravity(Gravity.CENTER);
    cover.setTextColor(android.graphics.Color.WHITE);
    cover.setTextSize(22);
    cover.setBackgroundColor(CoachUi.INK);
    cover.setText("평소처럼 편안하게 서 계세요\n촬영이 진행 중입니다");
    cover.setVisibility(View.GONE);
    frame.addView(cover, new FrameLayout.LayoutParams(-1, -1));
    status = CoachUi.text(body, "카메라 연결 중", 16, false);
    shutter = CoachUi.button(body, "촬영 시작", android.R.drawable.ic_menu_camera, this::startSequence);
    shutter.setEnabled(false);
    CoachUi.button(body, "취소", android.R.drawable.ic_menu_close_clear_cancel, this::preparation);
    ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
    future.addListener(() -> {
      if (!active || token != generation) return;
      try {
        provider = future.get();
        CameraSelector selector = new CameraSelector.Builder().requireLensFacing(
            state.camera.equals("전면") ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK).build();
        if (!provider.hasCamera(selector)) throw new IllegalStateException("Camera not available");
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Preview live = new Preview.Builder().setTargetRotation(rotation).build();
        live.setSurfaceProvider(preview.getSurfaceProvider());
        capture = new ImageCapture.Builder().setTargetRotation(rotation)
            .setTargetResolution(new Size(1280, 960))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
        provider.bindToLifecycle(this, selector, live, capture);
        updateLevel();
      } catch (Exception e) {
        status.setText("카메라를 열지 못했습니다. 권한과 선택한 카메라를 확인해 주세요.");
      }
    }, ContextCompat.getMainExecutor(this));
  }

  private void startSequence() {
    if (capture == null || shooting || !level) return;
    shooting = true;
    shutter.setEnabled(false);
    cover.setVisibility(View.VISIBLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    status.setText("잠시 편안하게 기다려 주세요");
    final int token = generation;
    handler.postDelayed(() -> take(token), 5000 + random.nextInt(2000));
  }

  private void take(int token) {
    if (!active || token != generation || capture == null) return;
    if (!level) { captureFailed("기기가 기울었습니다. 수평을 맞추고 다시 촬영해 주세요."); return; }
    capture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
      @Override public void onCaptureSuccess(@NonNull ImageProxy image) {
        Bitmap photo = null;
        try {
          ByteBuffer buffer = image.getPlanes()[0].getBuffer();
          byte[] jpeg = new byte[buffer.remaining()];
          buffer.get(jpeg);
          Bitmap raw = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
          if (raw != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(image.getImageInfo().getRotationDegrees());
            if (state.camera.equals("전면")) matrix.postScale(-1, 1);
            photo = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
            if (photo != raw) raw.recycle();
          }
        } catch (RuntimeException e) {
          // A failed conversion must not leave the camera image owned by the app.
        } finally { image.close(); }
        final Bitmap ready = photo;
        runOnUiThread(() -> {
          if (!active || token != generation) return;
          if (ready == null) { captureFailed("사진 처리에 실패했습니다. 다시 촬영해 주세요."); return; }
          if (!level) { captureFailed("촬영 중 기기가 기울었습니다. 다시 촬영해 주세요."); return; }
          state.photos.add(ready);
          if (state.photos.size() < 3) {
            handler.postDelayed(() -> take(token), 1500 + random.nextInt(2000));
          } else {
            stopCamera();
            state.index = 0;
            annotate();
          }
        });
      }
      @Override public void onError(@NonNull ImageCaptureException exception) {
        runOnUiThread(() -> {
          if (active && token == generation) captureFailed("촬영하지 못했습니다. 다시 시도해 주세요.");
        });
      }
    });
  }

  private void captureFailed(String message) { preparation(); error(message); }

  private void annotate() {
    state.screen = "annotate";
    final int token = ++generation;
    final int index = state.index;
    final Bitmap bitmap = state.photos.get(index);
    LinearLayout body = CoachUi.page(this, "기준점 확인  " + (index + 1) + " / 3");
    PosturePhotoView photo = new PosturePhotoView(this);
    photo.photo(bitmap, state.ears[index], state.bases[index]);
    int height = Math.max(260, Math.min(520, getResources().getConfiguration().screenHeightDp / 2));
    body.addView(photo, new LinearLayout.LayoutParams(-1, CoachUi.dp(this, height)));
    TextView hint = CoachUi.text(body, "귀 후보 확인 중 · C7은 직접 확인한 표식", 15, false);
    RadioGroup points = CoachUi.options(body, new String[]{"귀 (이주)", "C7 (확인한 표식)"}, 0);
    points.setOnCheckedChangeListener((group, id) -> photo.editingEar = CoachUi.selected(group) == 0);
    LinearLayout arrows = new LinearLayout(this);
    body.addView(arrows);
    String[] labels = {"←", "↑", "↓", "→"};
    String[] descriptions = {"기준점 왼쪽 이동", "기준점 위 이동", "기준점 아래 이동", "기준점 오른쪽 이동"};
    int[] dx = {-1, 0, 0, 1}, dy = {0, -1, 1, 0};
    for (int i = 0; i < 4; i++) {
      final int direction = i;
      Button arrow = new Button(this);
      arrow.setText(labels[i]);
      arrow.setContentDescription(descriptions[i]);
      arrow.setOnClickListener(v -> photo.nudge(dx[direction], dy[direction]));
      arrows.addView(arrow, new LinearLayout.LayoutParams(0, CoachUi.dp(this, 52), 1));
    }
    CheckBox verified = new CheckBox(this);
    verified.setText("귀·C7 위치와 자연스러운 옆모습을 확인했어요");
    verified.setTextColor(CoachUi.INK);
    body.addView(verified);
    Button next = CoachUi.button(body, index == 2 ? "결과 확인" : "다음 사진",
        android.R.drawable.ic_media_next, () -> {
          double angle = photo.angle();
          if (!verified.isChecked() || Double.isNaN(angle)) return;
          state.ears[index] = photo.ear;
          state.bases[index] = photo.base;
          state.angles[index] = angle;
          if (index < 2) { state.index++; annotate(); } else result();
        });
    next.setEnabled(false);
    photo.changed = () -> {
      state.ears[index] = photo.ear;
      state.bases[index] = photo.base;
      verified.setChecked(false);
      next.setEnabled(false);
      hint.setText(Double.isNaN(photo.angle()) ? "귀와 C7 기준점을 확인해 주세요" : "기준점 선택됨 · 위치 확인 필요");
    };
    verified.setEnabled(state.quality[index] > 0);
    verified.setOnCheckedChangeListener((button, checked) -> {
      boolean valid = !Double.isNaN(photo.angle());
      next.setEnabled(checked && valid && state.quality[index] > 0);
      if (checked && !valid) hint.setText("귀가 C7보다 위에 위치하도록 기준점을 다시 확인해 주세요");
    });
    if (index > 0) CoachUi.button(body, "이전 사진", android.R.drawable.ic_media_previous, () -> {
      state.index--; annotate();
    });
    CoachUi.button(body, "다시 촬영", android.R.drawable.ic_menu_camera, this::preparation);
    if (state.quality[index] != 0) {
      hint.setText(state.qualityMessage[index]);
      return;
    }
    detector.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener(this, faces -> {
      if (token != generation || !active) return;
      state.quality[index] = 1;
      if (faces.size() != 1) {
        state.quality[index] = faces.size() > 1 ? -1 : 1;
        state.qualityMessage[index] = faces.size() > 1
            ? "여러 사람이 보입니다. 한 사람만 나오도록 다시 촬영해 주세요."
            : "자동 자세 확인 불가 · 옆모습과 귀 위치를 직접 확인해 주세요.";
        hint.setText(state.qualityMessage[index]);
        verified.setEnabled(state.quality[index] > 0);
        return;
      }
      Face face = faces.get(0);
      int side = NeckPostureProcessor.sideForYaw(face.getHeadEulerAngleY());
      if (side == 0) {
        state.quality[index] = -1;
        state.qualityMessage[index] = "정면에 가까운 얼굴입니다. 옆모습으로 다시 촬영해 주세요.";
        hint.setText(state.qualityMessage[index]);
        verified.setEnabled(false);
        next.setEnabled(false);
        return;
      }
      FaceLandmark candidate = face.getLandmark(NeckPostureProcessor.faceEarTypeForPoseSide(side));
      if (photo.ear == null && candidate != null
          && NeckPostureAngle.isInsideImage(candidate.getPosition(), bitmap.getWidth(), bitmap.getHeight())) {
        photo.ear = new PointF(candidate.getPosition().x, candidate.getPosition().y);
        photo.changed.run();
        photo.invalidate();
        state.qualityMessage[index] = "ML Kit 귀 후보 · 이주 위치를 확인해 주세요";
      } else state.qualityMessage[index] = "귀와 C7 위치를 직접 확인해 주세요";
      hint.setText(state.qualityMessage[index]);
      verified.setEnabled(true);
    }).addOnFailureListener(this, error -> {
      if (token == generation && active) {
        state.quality[index] = 1;
        state.qualityMessage[index] = "자동 검출 실패 · 옆모습과 귀·C7을 직접 확인해 주세요";
        hint.setText(state.qualityMessage[index]);
        verified.setEnabled(true);
      }
    });
  }

  private void result() {
    ++generation;
    state.screen = "result";
    LinearLayout body = CoachUi.page(this, "사진 점검 결과");
    double median = CvaMath.median(state.angles);
    boolean consistent = CvaMath.repeatable(state.angles);
    CoachUi.text(body, String.format(Locale.KOREA, "CVA %.1f°", median), 36, true);
    CoachUi.text(body, String.format(Locale.KOREA, "%.1f°  ·  %.1f°  ·  %.1f°\n3회 측정의 중앙값",
        state.angles[0], state.angles[1], state.angles[2]), 17, false);
    CoachUi.text(body, consistent ? "반복 측정값 확인 완료" : "측정 차이가 커서 비교를 보류합니다", 20, true);
    CoachUi.text(body, consistent
        ? "이 값은 직접 확인한 귀·C7 기준의 참고값입니다. 수치만으로 거북목 진단이나 개선 여부를 확정하지 않습니다."
        : "촬영 자세나 기준점이 달라졌을 수 있습니다. 5° 편차 기준은 임시 품질 기준이며 임상 기준이 아닙니다.", 15, false);
    CoachUi.text(body, "서 있는 옆모습 · " + state.side + " · " + state.camera + " 카메라", 15, false);
    CoachUi.text(body, "의도적으로 턱을 당긴 자세인지 사진만으로 확인할 수 없습니다.", 14, false);
    if (consistent) {
      CoachUi.button(body, state.savedId >= 0 ? "저장 완료 · 오늘로" : "측정값 저장",
          android.R.drawable.ic_menu_save, () -> {
            try {
              if (state.savedId < 0) state.savedId = store.saveMeasurement(state.angles, state.protocol());
              state.clearPhotos();
              finish();
            } catch (RuntimeException e) { error("측정값을 저장하지 못했습니다. 다시 시도해 주세요."); }
          });
    }
    CoachUi.button(body, "기준점 다시 확인", android.R.drawable.ic_menu_edit, () -> {
      state.index = 0;
      annotate();
    });
    CoachUi.button(body, "다시 촬영", android.R.drawable.ic_menu_camera, this::preparation);
    CoachUi.button(body, "저장하지 않고 종료", android.R.drawable.ic_menu_close_clear_cancel, this::finish);
  }

  private void stopCamera() {
    generation++;
    handler.removeCallbacksAndMessages(null);
    if (provider != null) provider.unbindAll();
    capture = null;
    shooting = false;
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override public void onSensorChanged(SensorEvent event) {
    float x = event.values[0], y = event.values[1], z = event.values[2];
    int rotation = getWindowManager().getDefaultDisplay().getRotation();
    float across = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? y : x;
    float vertical = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? x : y;
    double roll = Math.toDegrees(Math.atan2(Math.abs(across), Math.abs(vertical)));
    double tilt = Math.toDegrees(Math.atan2(Math.abs(z), Math.hypot(x, y)));
    // Camera alignment tolerances, not clinical posture thresholds.
    level = roll <= 5 && tilt <= 15 && Math.hypot(x, y) > 5;
    updateLevel();
  }

  private void updateLevel() {
    if (!state.screen.equals("camera") || status == null || shutter == null || capture == null) return;
    if (shooting) return;
    status.setText(gravity == null ? "기기 수평을 직접 확인해 주세요" : level ? "기기 수평 확인됨" : "기기를 수평으로 세워 주세요");
    shutter.setEnabled(level);
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
  private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
  @Override protected void onPause() {
    active = false;
    stopCamera();
    sensors.unregisterListener(this);
    level = gravity == null;
    super.onPause();
  }
  @Override protected void onDestroy() {
    detector.close();
    executor.shutdown();
    store.close();
    super.onDestroy();
  }
  @Override public void onBackPressed() {
    new AlertDialog.Builder(this).setMessage("이번 사진 점검을 종료할까요?")
        .setPositiveButton("종료", (dialog, which) -> { state.clearPhotos(); finish(); })
        .setNegativeButton("계속하기", null).show();
  }
}
