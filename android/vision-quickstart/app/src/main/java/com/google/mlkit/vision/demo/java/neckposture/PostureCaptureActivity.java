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
import android.os.SystemClock;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.app.NotificationManager;
import android.view.HapticFeedbackConstants;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.AspectRatio;
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
  private FaceDetector framingDetector;
  private ImageAnalysis analysis;
  private CaptureGuideView guide;
  private final CaptureReadiness readiness = new CaptureReadiness();
  private CheckBox automatic;
  private ToneGenerator tones;
  private long lastAnalysis, lastFramed = -1, lastLevelCue = -5000;
  private boolean framed, previousLevel, automaticSequence;
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
    framingDetector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setMinFaceSize(0.08f).build());
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
    LinearLayout body = CoachUi.page(this, "사진 점검", this::finish);
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
    CheckBox auto = new CheckBox(this);
    auto.setText("구도와 수평이 맞으면 자동 촬영");
    auto.setTextColor(CoachUi.INK);
    auto.setMinHeight(CoachUi.dp(this, 48));
    auto.setChecked(state.autoCapture);
    auto.setOnCheckedChangeListener((button, checked) -> state.autoCapture = checked);
    body.addView(auto);
    CheckBox feedback = new CheckBox(this);
    feedback.setText("촬영 소리·진동 알림");
    feedback.setTextColor(CoachUi.INK);
    feedback.setMinHeight(CoachUi.dp(this, 48));
    feedback.setChecked(state.captureFeedback);
    feedback.setOnCheckedChangeListener((button, checked) -> state.captureFeedback = checked);
    body.addView(feedback);
    CoachUi.text(body, "안내음을 들으려면 미디어 소리를 켜 주세요. 무음·방해금지 설정과 기기 진동 설정을 따릅니다.", 14, false);
    Button next = CoachUi.button(body, "카메라 열기", android.R.drawable.ic_menu_camera, () -> {
      state.side = CoachUi.selected(side) == 0 ? "왼쪽" : "오른쪽";
      state.camera = CoachUi.selected(camera) == 0 ? "후면" : "전면";
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 7);
      } else camera();
    });
    CoachUi.styleButton(next, true);
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
    LinearLayout body = CoachUi.page(this, "자연스러운 옆모습", this::onBackPressed);
    FrameLayout frame = new FrameLayout(this);
    int height = Math.max(180, Math.min(360, getResources().getConfiguration().screenHeightDp * 2 / 5));
    body.addView(frame, new LinearLayout.LayoutParams(-1, CoachUi.dp(this, height)));
    preview = new PreviewView(this);
    preview.setScaleType(PreviewView.ScaleType.FIT_CENTER);
    frame.addView(preview, new FrameLayout.LayoutParams(-1, -1));
    guide = new CaptureGuideView(this);
    frame.addView(guide, new FrameLayout.LayoutParams(-1, -1));
    cover = new TextView(this);
    cover.setGravity(Gravity.CENTER);
    cover.setTextColor(android.graphics.Color.WHITE);
    cover.setTextSize(22);
    cover.setBackgroundColor(CoachUi.INK);
    cover.setText("평소처럼 편안하게 서 계세요\n촬영이 진행 중입니다");
    cover.setVisibility(View.GONE);
    frame.addView(cover, new FrameLayout.LayoutParams(-1, -1));
    status = CoachUi.text(body, "카메라 연결 중", 16, false);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    CoachUi.text(body, "위 실선: 옆얼굴 · 아래 점선: 목과 C7 표식\nC7 위치는 촬영 후 직접 확인합니다.", 14, false);
    automatic = new CheckBox(this);
    automatic.setText("자동 촬영");
    automatic.setTextColor(CoachUi.INK);
    automatic.setMinHeight(CoachUi.dp(this, 48));
    automatic.setChecked(state.autoCapture);
    automatic.setOnCheckedChangeListener((button, checked) -> {
      state.autoCapture = checked;
      readiness.reset();
      updateLevel();
    });
    body.addView(automatic);
    shutter = CoachUi.button(body, "수동 촬영 시작", android.R.drawable.ic_menu_camera, () -> startSequence(false));
    CoachUi.styleButton(shutter, true);
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
        Preview live = new Preview.Builder().setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3).build();
        live.setSurfaceProvider(preview.getSurfaceProvider());
        capture = new ImageCapture.Builder().setTargetRotation(rotation)
            .setTargetResolution(new Size(1280, 960))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
        analysis = new ImageAnalysis.Builder().setTargetRotation(rotation)
            .setTargetResolution(new Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        analysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> analyzeFrame(image, token));
        try {
          provider.bindToLifecycle(this, selector, live, capture, analysis);
        } catch (IllegalArgumentException | IllegalStateException unsupported) {
          analysis.clearAnalyzer();
          analysis = null;
          provider.unbindAll();
          provider.bindToLifecycle(this, selector, live, capture);
          automatic.setChecked(false);
          automatic.setEnabled(false);
          error("이 카메라에서는 자동 구도 확인을 사용할 수 없어 수동 촬영으로 전환했습니다.");
        }
        updateLevel();
        handler.postDelayed(() -> watchFraming(token), 300);
      } catch (Exception e) {
        status.setText("카메라를 열지 못했습니다. 권한과 선택한 카메라를 확인해 주세요.");
      }
    }, ContextCompat.getMainExecutor(this));
  }

  private void startSequence(boolean auto) {
    if (capture == null || shooting || !level) return;
    if (auto && (gravity == null || !freshFrame())) return;
    automaticSequence = auto;
    shooting = true;
    readiness.reset();
    automatic.setEnabled(false);
    shutter.setEnabled(false);
    cover.setVisibility(View.VISIBLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    status.setText("잠시 편안하게 기다려 주세요");
    signal(ToneGenerator.TONE_PROP_ACK);
    final int token = generation;
    handler.postDelayed(() -> take(token), 5000 + random.nextInt(2000));
  }

  private void take(int token) {
    if (!active || token != generation || capture == null) return;
    if (!level) { captureFailed("기기가 기울었습니다. 수평을 맞추고 다시 촬영해 주세요."); return; }
    if (automaticSequence && !freshFrame()) {
      captureFailed("구도를 벗어났습니다. 얼굴과 표식이 보이도록 다시 준비해 주세요."); return;
    }
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
          if (automaticSequence && !freshFrame()) {
            captureFailed("촬영 중 구도가 달라졌습니다. 다시 준비해 주세요."); return;
          }
          state.photos.add(ready);
          signal(state.photos.size() == 3 ? ToneGenerator.TONE_PROP_PROMPT : ToneGenerator.TONE_PROP_BEEP2);
          status.setText("사진 " + state.photos.size() + " / 3 촬영 완료");
          cover.setText("사진 " + state.photos.size() + " / 3\n평소처럼 편안하게 서 계세요");
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

  private void captureFailed(String message) {
    preparation();
    signal(ToneGenerator.TONE_PROP_NACK);
    error(message);
  }

  private boolean freshFrame() {
    return framed && lastFramed >= 0 && SystemClock.elapsedRealtime() - lastFramed <= CaptureReadiness.FRESH_MS;
  }

  @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
  private void analyzeFrame(ImageProxy image, int token) {
    long now = SystemClock.elapsedRealtime();
    if (!active || token != generation || now - lastAnalysis < 250 || image.getImage() == null) {
      image.close(); return;
    }
    lastAnalysis = now;
    int rotation = image.getImageInfo().getRotationDegrees();
    int width = rotation % 180 == 0 ? image.getWidth() : image.getHeight();
    int height = rotation % 180 == 0 ? image.getHeight() : image.getWidth();
    try {
      framingDetector.process(InputImage.fromMediaImage(image.getImage(), rotation))
          .addOnSuccessListener(ContextCompat.getMainExecutor(this), faces -> {
            if (!active || token != generation) return;
            framed = false;
            if (faces.size() == 1 && NeckPostureProcessor.sideForYaw(faces.get(0).getHeadEulerAngleY()) != 0) {
              android.graphics.Rect box = faces.get(0).getBoundingBox();
              framed = CaptureReadiness.framed(box.left / (float) width, box.top / (float) height,
                  box.right / (float) width, box.bottom / (float) height);
            }
            framed &= SystemClock.elapsedRealtime() - now <= CaptureReadiness.FRESH_MS;
            if (framed) lastFramed = now;
            guide.update(width, height, framed && level);
            if (!shooting) {
              boolean ready = readiness.update(SystemClock.elapsedRealtime(), level, framed);
              updateLevel();
              if (ready && state.autoCapture && gravity != null) startSequence(true);
            }
          }).addOnFailureListener(ContextCompat.getMainExecutor(this), error -> {
            if (token == generation) { framed = false; readiness.reset(); updateLevel(); }
          }).addOnCompleteListener(task -> image.close());
    } catch (RuntimeException e) {
      image.close();
      framed = false;
      readiness.reset();
    }
  }

  private void watchFraming(int token) {
    if (!active || token != generation || capture == null) return;
    if (shooting && automaticSequence && (!level || !freshFrame())) {
      captureFailed("구도 또는 수평이 유지되지 않았습니다. 다시 준비해 주세요."); return;
    }
    if (!freshFrame()) readiness.reset();
    if (!shooting) updateLevel();
    handler.postDelayed(() -> watchFraming(token), 300);
  }

  private void signal(int tone) {
    if (!state.captureFeedback) return;
    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (android.os.Build.VERSION.SDK_INT >= 23
        && notifications.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) return;
    if (audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT) return;
    getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
        || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return;
    try {
      if (tones == null) tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
      tones.startTone(tone, 180);
    } catch (RuntimeException ignored) { /* Sound may be unavailable; visual feedback remains. */ }
  }

  private void annotate() {
    state.screen = "annotate";
    final int token = ++generation;
    final int index = state.index;
    final Bitmap bitmap = state.photos.get(index);
    LinearLayout body = CoachUi.page(this, "기준점 확인  " + (index + 1) + " / 3", this::onBackPressed);
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
      arrow.setGravity(Gravity.CENTER);
      arrow.setMinimumWidth(0);
      arrow.setPadding(0, 0, 0, 0);
      CoachUi.styleButton(arrow, false);
      arrow.setOnClickListener(v -> photo.nudge(dx[direction], dy[direction]));
      arrows.addView(arrow, new LinearLayout.LayoutParams(0, CoachUi.dp(this, 52), 1));
    }
    CheckBox verified = new CheckBox(this);
    verified.setText("귀·C7 위치와 자연스러운 옆모습을 확인했어요");
    verified.setTextColor(CoachUi.INK);
    verified.setMinHeight(CoachUi.dp(this, 48));
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
    CoachUi.styleButton(next, true);
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
    LinearLayout body = CoachUi.page(this, "사진 점검 결과", this::onBackPressed);
    double mean = CvaMath.mean(state.angles);
    boolean consistent = CvaMath.repeatable(state.angles);
    LinearLayout summary = CoachUi.card(body, true);
    CoachUi.text(summary, "대표 각도", 14, false).setGravity(Gravity.CENTER);
    TextView angle = CoachUi.text(summary, String.format(Locale.KOREA, "%.1f°", mean), 64, true);
    angle.setTextColor(CoachUi.GREEN);
    angle.setGravity(Gravity.CENTER);
    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(angle, 32, 64, 1,
        android.util.TypedValue.COMPLEX_UNIT_SP);
    CoachUi.text(summary, "세 값의 평균값", 14, false).setGravity(Gravity.CENTER);
    CoachUi.text(body, "사진별 측정값", 20, true);
    LinearLayout samples = new LinearLayout(this);
    boolean stacked = getResources().getConfiguration().fontScale > 1.3f;
    samples.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
    body.addView(samples, new LinearLayout.LayoutParams(-1, -2));
    String[] labels = {"첫 사진", "두 번째", "세 번째"};
    for (int i = 0; i < 3; i++) {
      LinearLayout sample = CoachUi.card(samples, false);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(stacked ? -1 : 0, -2, stacked ? 0 : 1);
      params.setMargins(CoachUi.dp(this, 4), CoachUi.dp(this, 8), CoachUi.dp(this, 4), CoachUi.dp(this, 8));
      sample.setLayoutParams(params);
      sample.setPadding(CoachUi.dp(this, 8), CoachUi.dp(this, 12), CoachUi.dp(this, 8), CoachUi.dp(this, 12));
      CoachUi.text(sample, labels[i], 14, false).setGravity(Gravity.CENTER);
      TextView value = CoachUi.text(sample, String.format(Locale.KOREA, "%.1f°", state.angles[i]), 22, true);
      value.setGravity(Gravity.CENTER);
    }
    CoachUi.text(body, consistent ? "반복 측정값 확인 완료" : "측정 차이가 커서 비교를 보류합니다", 20, true);
    CoachUi.text(body, consistent
        ? "이 값은 직접 확인한 귀·C7 기준의 참고값입니다. 수치만으로 거북목 진단이나 개선 여부를 확정하지 않습니다."
        : "촬영 자세나 기준점이 달라졌을 수 있습니다. 5° 편차 기준은 임시 품질 기준이며 임상 기준이 아닙니다.", 15, false);
    LinearLayout conditions = CoachUi.card(body, false);
    CoachUi.text(conditions, "촬영 조건", 16, true);
    CoachUi.text(conditions, "서 있는 옆모습 · " + state.side + " · " + state.camera + " 카메라", 15, false);
    CoachUi.text(body, "의도적으로 턱을 당긴 자세인지 사진만으로 확인할 수 없습니다.", 14, false);
    if (consistent) {
      CoachUi.styleButton(CoachUi.button(body, state.savedId >= 0 ? "저장 완료 · 오늘로" : "결과 저장",
          android.R.drawable.ic_menu_save, () -> {
            try {
              if (state.savedId < 0) state.savedId = store.saveMeasurement(state.angles, state.protocol());
              state.clearPhotos();
              finish();
            } catch (RuntimeException e) { error("측정값을 저장하지 못했습니다. 다시 시도해 주세요."); }
          }), true);
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
    if (analysis != null) { analysis.clearAnalyzer(); analysis = null; }
    if (provider != null) provider.unbindAll();
    capture = null;
    shooting = false;
    framed = false;
    lastFramed = -1;
    lastAnalysis = 0;
    previousLevel = false;
    readiness.reset();
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
    if (!level) readiness.reset();
    updateLevel();
  }

  private void updateLevel() {
    if (!state.screen.equals("camera") || status == null || shutter == null || capture == null) return;
    if (shooting) return;
    long now = SystemClock.elapsedRealtime();
    if (level && !previousLevel && now - lastLevelCue >= 3000) {
      signal(ToneGenerator.TONE_PROP_BEEP);
      lastLevelCue = now;
    }
    previousLevel = level;
    String message = gravity == null ? "수평 센서 없음 · 직접 확인 후 수동 촬영"
        : !level ? "기기를 수평으로 세워 주세요"
        : !state.autoCapture ? "수평 확인됨 · 수동 촬영을 시작하세요"
        : freshFrame() ? "구도·수평 확인됨 · 잠시 유지하면 자동 촬영"
        : "수평 확인됨 · 위 틀에 옆얼굴을 맞춰 주세요";
    if (!message.contentEquals(status.getText())) status.setText(message);
    shutter.setEnabled(level);
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
  private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
  @Override protected void onPause() {
    active = false;
    stopCamera();
    if (tones != null) { tones.release(); tones = null; }
    sensors.unregisterListener(this);
    level = gravity == null;
    super.onPause();
  }
  @Override protected void onDestroy() {
    detector.close();
    framingDetector.close();
    executor.shutdown();
    store.close();
    super.onDestroy();
  }
  @Override public void onBackPressed() {
    boolean wasCamera = state.screen.equals("camera");
    if (wasCamera) { stopCamera(); state.clearPhotos(); state.screen = "prepare"; }
    new AlertDialog.Builder(this).setMessage("이번 사진 점검을 종료할까요?")
        .setPositiveButton("종료", (dialog, which) -> { state.clearPhotos(); finish(); })
        .setNegativeButton("계속하기", (dialog, which) -> {
          if (wasCamera) preparation();
        }).setOnCancelListener(dialog -> {
          if (wasCamera) preparation();
        }).show();
  }
}
