package com.google.mlkit.vision.demo.java.neckposture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local habit plan, routine timer, and scheduled posture check-ins. */
public final class PostureCoachActivity extends AppCompatActivity {
  private PostureStore store;
  private PostureSession state;
  private CountDownTimer timer;
  private TextView clock, movement, movementDetail;
  private Button play;
  private ExerciseArtworkView artwork;
  private android.widget.EditText practiceDraft;
  private int previewExercise;
  private static final String EXERCISE_SOURCE =
      "https://www.cuh.nhs.uk/patient-information/neck-exercises-and-advice/";

  @Override protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
      @Override public void handleOnBackPressed() { navigateBack(); }
    });
    getWindow().setStatusBarColor(CoachUi.GREEN);
    store = new PostureStore(this);
    state = new ViewModelProvider(this).get(PostureSession.class);
  }

  @Override protected void onResume() {
    super.onResume();
    if (!getSharedPreferences("moklog_privacy", MODE_PRIVATE).getBoolean("ack_v1", false)) welcome();
    else if (!store.hasPlan()) setup();
    else if (state.screen.equals("routine")) routine();
    else if (state.screen.equals("history")) history();
    else if (state.screen.equals("settings")) settings();
    else if (state.screen.equals("exercises")) exercises();
    else if (state.screen.equals("confirm-practice")) confirmPractice();
    else if (state.screen.equals("completed")) completed();
    else home();
  }

  private void welcome() {
    state.screen = "welcome";
    LinearLayout body = CoachUi.page(this, "목log");
    android.widget.ImageView logo = new android.widget.ImageView(this);
    logo.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
    logo.setContentDescription("목log 로고");
    LinearLayout.LayoutParams image = new LinearLayout.LayoutParams(CoachUi.dp(this, 104), CoachUi.dp(this, 104));
    image.gravity = android.view.Gravity.CENTER_HORIZONTAL;
    image.setMargins(0, CoachUi.dp(this, 16), 0, CoachUi.dp(this, 24));
    body.addView(logo, image);
    CoachUi.text(body, "작은 실천을 기록하고,\n같은 조건으로 자세를 살펴보세요.", 24, true);
    CoachUi.text(body, "사진은 기기에서만 분석합니다. 운동 날짜와 각도만 기기에 기록합니다. ML Kit은 기기·성능 진단 정보를 처리할 수 있습니다.", 16, false);
    CoachUi.text(body, "의료 진단 앱이 아닙니다. 귀·C7 기준점은 직접 확인하며 운동은 편안한 범위에서 진행하세요.", 16, false);
    CoachUi.button(body, "개인정보·이용 안내", android.R.drawable.ic_menu_info_details,
        () -> startActivity(new Intent(this, PrivacyActivity.class)));
    CheckBox checked = new CheckBox(this);
    checked.setText("개인정보 처리와 자가 점검의 한계를 확인했어요");
    checked.setTextColor(CoachUi.INK); checked.setMinHeight(CoachUi.dp(this, 48));
    body.addView(checked);
    Button next = CoachUi.button(body, "시작하기", android.R.drawable.ic_media_play, () -> {
      if (!getSharedPreferences("moklog_privacy", MODE_PRIVATE).edit().putBoolean("ack_v1", true).commit()) {
        error("설정을 저장하지 못했습니다."); return;
      }
      if (store.hasPlan()) home(); else setup();
    });
    CoachUi.styleButton(next, true); next.setEnabled(false);
    checked.setOnCheckedChangeListener((button, value) -> next.setEnabled(value));
  }

  private void setup() {
    state.screen = "setup";
    LinearLayout body = CoachUi.page(this, "목log");
    CoachUi.text(body, "나의 운동 습관", 20, true);
    CoachUi.text(body, "기본 제안: 하루 3분, 4주\n치료 기간 예측이 아닌 실천 목표입니다.", 16, false);
    CoachUi.text(body, "목표 기간", 16, true);
    RadioGroup weeks = CoachUi.options(body, new String[]{"2주", "4주"}, 1);
    CoachUi.text(body, "하루에 쓸 시간", 16, true);
    RadioGroup minutes = CoachUi.options(body, new String[]{"3분", "5분"}, 0);
    CoachUi.text(body, "사진 점검은 시작 · 중간 · 마지막에 진행합니다.\n사진은 저장하지 않고 기기에 측정값과 실천 기록만 남깁니다.", 15, false);
    CoachUi.styleButton(CoachUi.button(body, "이 목표로 시작", android.R.drawable.ic_media_play, () -> {
      try {
        store.createPlan(CoachUi.selected(weeks) == 0 ? 2 : 4,
            CoachUi.selected(minutes) == 0 ? 3 : 5);
        state.routineRemaining = 0;
        state.practiceNote = "";
        home();
      } catch (RuntimeException e) { error("목표를 저장하지 못했습니다. 다시 시도해 주세요."); }
    }), true);
    if (store.hasPlan()) CoachUi.button(body, "돌아가기", android.R.drawable.ic_menu_revert, this::home);
  }

  private void home() {
    pauseTimer();
    state.screen = "home";
    LinearLayout body = CoachUi.page(this, "목log");
    int total = store.weeks() * 7;
    int day = store.day();
    List<String> days = store.practiceDays();
    List<String> allDays = store.practiceDays(true);
    CoachUi.text(body, "오늘도 가볍게,\n꾸준히 실천해요.", 24, true);
    LinearLayout goal = CoachUi.card(body, true);
    CoachUi.text(goal, day > total ? "목표 기간을 마쳤어요" : "나의 목표 · " + day + "일차", 14, false);
    CoachUi.text(goal, store.weeks() + "주 동안 하루 " + store.minutes() + "분", 20, true);
    ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(total);
    progress.setProgress(Math.min(days.size(), total));
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CoachUi.GREEN));
    goal.addView(progress, new LinearLayout.LayoutParams(-1, CoachUi.dp(this, 8)));
    CoachUi.text(goal, "실천 " + days.size() + "일 / 목표 " + total + "일", 14, false);
    LinearLayout practice = CoachUi.card(body, false);
    boolean done = store.practicedToday();
    CoachUi.text(practice, done ? "오늘의 실천 완료" : "오늘의 루틴", 20, true);
    CoachUi.text(practice, done ? "오늘의 기록을 남겼어요. 편안하게 쉬어 가세요."
        : "가볍게 턱 당기기 · 편안하게 목 돌리기", 14, false);
    if (!store.habitCue().isEmpty()) CoachUi.text(practice, "나의 실천 약속 · " + store.habitCue(), 14, false);
    CoachUi.styleButton(CoachUi.button(practice, done ? "루틴 다시 보기" : "운동 시작",
        android.R.drawable.ic_media_play, this::routine), true);
    CoachUi.button(practice, store.habitCue().isEmpty() ? "실천할 순간 정하기" : "실천 약속 바꾸기",
        android.R.drawable.ic_menu_my_calendar, this::habitCue);
    LinearLayout week = CoachUi.card(body, false);
    CoachUi.text(week, "이번 주 실천", 20, true);
    int weeklyCount = HabitProgress.completedThisWeek(allDays, PostureStore.today());
    CoachUi.text(week, weeklyCount == 0 ? "이번 주 첫 실천을 남겨 보세요"
        : weeklyCount + "일의 작은 실천을 쌓았어요", 14, false);
    CoachUi.week(week, allDays, PostureStore.today());
    LinearLayout photo = CoachUi.card(body, false);
    CoachUi.text(photo, "사진으로 자세 살펴보기", 20, true);
    List<PostureStore.Reading> readings = store.readings();
    int middle = total / 2;
    boolean midDone = false, endDone = false;
    for (PostureStore.Reading reading : readings) {
      String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(reading.created));
      int measuredDay = PostureStore.elapsedDays(store.start(), date) + 1;
      midDone |= measuredDay >= middle;
      endDone |= measuredDay >= total;
    }
    String checkpoint = readings.isEmpty() ? "시작 점검이 남아 있어요"
        : day >= total && !endDone ? "마지막 점검을 할 때예요"
        : day >= middle && !midDone ? "중간 점검을 할 때예요"
        : day < middle ? "다음 점검: " + middle + "일째"
        : !endDone ? "다음 점검: " + total + "일째" : "목표 기간의 점검을 마쳤어요";
    CoachUi.text(photo, checkpoint, 14, false);
    if (!readings.isEmpty()) {
      CoachUi.text(photo, String.format(Locale.KOREA, "최근 평균 CVA %.1f°", readings.get(0).mean), 24, true);
      CoachUi.text(photo, "직접 확인한 귀·C7 기준의 자가 참고값", 14, false);
    }
    CoachUi.button(photo, "사진 점검", android.R.drawable.ic_menu_camera,
        () -> startActivity(new Intent(this, PostureCaptureActivity.class)));
    CoachUi.button(body, "지난 기록 보기", android.R.drawable.ic_menu_recent_history, this::history);
    CoachUi.button(body, "운동 둘러보기", android.R.drawable.ic_menu_view, this::exercises);
    if (day > total) CoachUi.button(body, "다음 목표 설정", android.R.drawable.ic_menu_edit, this::setup);
    CoachUi.navigation(body, 0, this::home, this::history, this::settings);
  }

  private void routine() {
    pauseTimer();
    state.screen = "routine";
    if (state.routineRemaining <= 0) state.routineRemaining = store.minutes() * 60;
    LinearLayout body = CoachUi.page(this, "오늘의 운동", this::home);
    CoachUi.text(body, "일반 운동 안내 · 편안한 범위에서", 15, false);
    CoachUi.text(body, ExerciseLibrary.SAFETY, 15, false);
    movement = CoachUi.text(body, "", 22, true);
    movementDetail = CoachUi.text(body, "", 17, false);
    artwork = new ExerciseArtworkView(this, ExerciseLibrary.stage(state.routineRemaining, store.minutes() * 60));
    body.addView(artwork, new LinearLayout.LayoutParams(-1, -2));
    CoachUi.text(body, "그림은 동작 참고용입니다. 쉬어 가며 편안하게 움직이세요.", 14, false);
    clock = CoachUi.timer(body);
    CoachUi.text(body, "표시 시간에는 휴식도 포함됩니다. 같은 자세를 계속 버티지 말고 무리 없이 움직이세요.", 15, false);
    CheckBox ready = new CheckBox(this);
    ready.setText("오늘 편안하게 운동할 수 있는 상태예요");
    ready.setTextColor(CoachUi.INK);
    ready.setMinHeight(CoachUi.dp(this, 48));
    body.addView(ready);
    play = CoachUi.button(body, "시작", android.R.drawable.ic_media_play, () -> {
      if (state.routineRunning) pauseTimer(); else startTimer();
    });
    CoachUi.styleButton(play, true);
    play.setEnabled(false);
    ready.setOnCheckedChangeListener((button, checked) -> {
      if (!checked) pauseTimer();
      play.setEnabled(checked);
    });
    CoachUi.button(body, "운동 중단", android.R.drawable.ic_media_pause, () -> {
      pauseTimer();
      state.routineRemaining = 0;
      home();
    });
    CoachUi.button(body, "운동 안내 출처", android.R.drawable.ic_menu_info_details, () -> {
      try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(EXERCISE_SOURCE))); }
      catch (android.content.ActivityNotFoundException e) { error("브라우저를 찾지 못했습니다."); }
    });
    refreshRoutine();
  }

  private void refreshRoutine() {
    int remaining = state.routineRemaining;
    clock.setText(String.format(Locale.KOREA, "%02d:%02d", remaining / 60, remaining % 60));
    int stage = ExerciseLibrary.stage(remaining, store.minutes() * 60);
    movement.setText((stage + 1) + ". " + ExerciseLibrary.TITLES[stage]);
    movementDetail.setText(ExerciseLibrary.STEPS[stage] + "\n" + ExerciseLibrary.CAUTIONS[stage]);
    artwork.exercise(stage);
    artwork.playing(state.routineRunning);
    play.setText(state.routineRunning ? "일시정지" :
        state.routineRemaining == store.minutes() * 60 ? "시작" : "계속하기");
    CoachUi.buttonIcon(play, state.routineRunning ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
  }

  private void exercises() {
    pauseTimer();
    state.screen = "exercises";
    LinearLayout body = CoachUi.page(this, "운동 안내", this::home);
    CoachUi.text(body, ExerciseLibrary.SAFETY, 15, false);
    RadioGroup choices = CoachUi.options(body, ExerciseLibrary.TITLES, previewExercise);
    ExerciseArtworkView preview = new ExerciseArtworkView(this, previewExercise);
    artwork = preview;
    body.addView(preview, new LinearLayout.LayoutParams(-1, -2));
    TextView title = CoachUi.text(body, ExerciseLibrary.TITLES[previewExercise], 20, true);
    TextView steps = CoachUi.text(body, ExerciseLibrary.STEPS[previewExercise], 16, false);
    TextView caution = CoachUi.text(body, ExerciseLibrary.CAUTIONS[previewExercise], 14, false);
    choices.setOnCheckedChangeListener((group, id) -> {
      previewExercise = CoachUi.selected(group);
      preview.exercise(previewExercise);
      title.setText(ExerciseLibrary.TITLES[previewExercise]);
      steps.setText(ExerciseLibrary.STEPS[previewExercise]);
      caution.setText(ExerciseLibrary.CAUTIONS[previewExercise]);
    });
    CheckBox sequence = new CheckBox(this);
    sequence.setText("그림 순서 재생");
    sequence.setTextColor(CoachUi.INK); sequence.setMinHeight(CoachUi.dp(this, 48));
    body.addView(sequence);
    sequence.setOnCheckedChangeListener((button, value) -> preview.playing(value));
    CoachUi.text(body, "AI 보조로 자체 제작한 참고 그림 · 실제 시연 영상이 아닙니다.", 14, false);
    CoachUi.button(body, "공식 운동 안내 보기", android.R.drawable.ic_menu_info_details, () -> {
      try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ExerciseLibrary.SOURCE))); }
      catch (android.content.ActivityNotFoundException e) { error("브라우저를 찾지 못했습니다."); }
    });
    CoachUi.styleButton(CoachUi.button(body, "오늘의 운동 시작", android.R.drawable.ic_media_play, this::routine), true);
  }

  private void startTimer() {
    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    state.routineRunning = true;
    refreshRoutine();
    timer = new CountDownTimer(state.routineRemaining * 1000L, 250) {
      @Override public void onTick(long millis) {
        state.routineRemaining = (int) Math.ceil(millis / 1000.0);
        refreshRoutine();
      }
      @Override public void onFinish() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        state.routineRemaining = 0;
        state.routineRunning = false;
        timer = null;
        confirmPractice();
      }
    }.start();
  }

  private void confirmPractice() {
    state.screen = "confirm-practice";
    LinearLayout body = CoachUi.page(this, "오늘의 운동 기록", this::home);
    CoachUi.text(body, "오늘의 루틴을 마쳤나요?", 24, true);
    CoachUi.text(body, "실제로 수행한 경우에만 기록해 주세요. 표시 시간에는 휴식도 포함됩니다.", 16, false);
    if (store.practicedToday()) {
      CoachUi.text(body, "오늘은 이미 기록했어요. 같은 목표의 하루 실천은 한 번만 남습니다.", 16, false);
      CoachUi.styleButton(CoachUi.button(body, "오늘 기록 보기", android.R.drawable.ic_menu_recent_history, this::history), true);
    } else {
      CoachUi.text(body, "한 줄 기록 · 선택", 16, true);
      practiceDraft = CoachUi.input(body, state.practiceNote, "오늘의 느낌이나 다음에 기억할 점 (280자 이내)", 280);
      CoachUi.styleButton(CoachUi.button(body, "실천 기록하기", android.R.drawable.checkbox_on_background, () -> {
        state.practiceNote = practiceDraft.getText().toString();
        try {
          store.finishPractice(store.minutes() * 60, state.practiceNote);
          state.practiceNote = ""; completed();
        } catch (RuntimeException e) { error("기록을 저장하지 못했습니다. 다시 시도해 주세요."); }
      }), true);
    }
    CoachUi.button(body, "기록하지 않고 돌아가기", android.R.drawable.ic_menu_revert, () -> {
      state.practiceNote = ""; home();
    });
  }

  private void completed() {
    if (!store.practicedToday()) { home(); return; }
    state.screen = "completed";
    LinearLayout body = CoachUi.page(this, "오늘의 실천");
    TextView check = CoachUi.text(body, "✓", 48, true);
    check.setGravity(android.view.Gravity.CENTER); check.setTextColor(CoachUi.GREEN);
    CoachUi.text(body, "오늘도 한 걸음 쌓았어요", 24, true);
    CoachUi.text(body, PostureStore.today() + " · " + store.minutes()
        + "분 루틴을 기록했어요. 시간에는 휴식이 포함됩니다.", 16, false);
    CoachUi.week(body, store.practiceDays(true), PostureStore.today());
    CoachUi.text(body, "놓친 날이 있어도 괜찮아요. 다음 실천을 다시 이어 가세요.", 15, false);
    CoachUi.styleButton(CoachUi.button(body, "기록 보기", android.R.drawable.ic_menu_recent_history, this::history), true);
    CoachUi.button(body, "오늘로 돌아가기", android.R.drawable.ic_menu_revert, this::home);
  }

  private void habitCue() {
    String[] cues = {"점심 식사 후", "공부·업무를 마친 뒤", "잠들기 전", "직접 정하기", "실천 약속 지우기"};
    new AlertDialog.Builder(this).setTitle("어떤 순간에 실천할까요?")
        .setItems(cues, (dialog, index) -> {
          if (index == 3) {
            LinearLayout form = new LinearLayout(this);
            form.setOrientation(LinearLayout.VERTICAL);
            int pad = CoachUi.dp(this, 20); form.setPadding(pad, pad, pad, pad);
            android.widget.EditText input = CoachUi.input(form, store.habitCue(),
                "예: 점심 식사 후 책상 앞에서 (60자 이내)", 60);
            new AlertDialog.Builder(this).setTitle("나의 실천 약속").setView(form)
                .setPositiveButton("저장", (d, w) -> saveHabitCue(input.getText().toString()))
                .setNegativeButton("취소", null).show();
          } else saveHabitCue(index == 4 ? "" : cues[index]);
        }).setNegativeButton("취소", null).show();
  }

  private void saveHabitCue(String cue) {
    try { store.setHabitCue(cue); home(); }
    catch (RuntimeException e) { error("실천 약속을 저장하지 못했습니다."); }
  }

  private void history() {
    pauseTimer();
    state.screen = "history";
    LinearLayout body = CoachUi.page(this, "내 기록");
    RadioGroup tabs = CoachUi.options(body, new String[]{"운동 일지", "사진 점검"}, state.historyTab);
    tabs.setOnCheckedChangeListener((group, id) -> { state.historyTab = CoachUi.selected(group); history(); });
    if (state.historyTab == 0) practiceHistory(body); else photoHistory(body);
    CoachUi.navigation(body, 1, this::home, this::history, this::settings);
  }

  private void practiceHistory(LinearLayout body) {
    CoachUi.text(body, "모든 목표에서 쌓은 실천 기록", 15, false);
    CoachUi.text(body, "운동 실천", 20, true);
    List<String> days = store.practiceDays(true);
    List<PostureStore.Practice> practices = store.practices(true);
    long seconds = 0;
    for (PostureStore.Practice practice : practices) seconds += practice.seconds;
    CoachUi.text(body, "누적 " + days.size() + "일 · 기록 시간 " + seconds / 60 + "분", 24, true);
    CoachUi.text(body, "시간은 휴식이 포함된 기록된 루틴 길이입니다.", 14, false);
    CoachUi.week(body, days, PostureStore.today());
    if (days.isEmpty()) {
      CoachUi.text(body, "첫 실천을 기다리고 있어요", 20, true);
      CoachUi.text(body, "오늘의 루틴을 마치고 기록하면 달력과 운동 일지에 쌓여요.", 16, false);
      CoachUi.styleButton(CoachUi.button(body, "첫 루틴 시작하기", android.R.drawable.ic_media_play, this::routine), true);
    }
    for (int index = 0; index < Math.min(practices.size(), state.practiceLimit); index++) {
      PostureStore.Practice practice = practices.get(index);
      LinearLayout entry = CoachUi.card(body, false);
      CoachUi.text(entry, practice.day + " · " + practice.seconds / 60 + "분", 20, true);
      CoachUi.text(entry, practice.note.isEmpty() ? "실천 완료 · 한 줄 기록을 남겨 보세요" : practice.note, 16, false);
      CoachUi.button(entry, practice.note.isEmpty() ? "한 줄 기록 남기기" : "한 줄 기록 수정",
          android.R.drawable.ic_menu_edit, () -> editPractice(practice));
    }
    if (practices.size() > state.practiceLimit) CoachUi.button(body, "이전 운동 일지 더 보기",
        android.R.drawable.ic_menu_more, () -> { state.practiceLimit += 30; history(); });
  }

  private void photoHistory(LinearLayout body) {
    CoachUi.text(body, "대표 각도는 사진 3장의 평균입니다. 이전 기록도 원래의 사진별 값으로 평균을 계산해 표시합니다.", 14, false);
    CoachUi.text(body, "사진 점검", 20, true);
    List<PostureStore.Reading> readings = store.readings(true);
    if (readings.isEmpty()) CoachUi.text(body, "아직 측정 기록이 없어요.", 16, false);
    for (int i = 0; i < readings.size(); i++) {
      PostureStore.Reading reading = readings.get(i);
      String when = new SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREA).format(new Date(reading.created));
      LinearLayout entry = CoachUi.card(body, false);
      CoachUi.text(entry, String.format(Locale.KOREA, "%s   평균 %.1f°", when, reading.mean), 20, true);
      CoachUi.text(entry, String.format(Locale.KOREA, "3회 측정  %.1f° · %.1f° · %.1f°\n%s",
          reading.samples[0], reading.samples[1], reading.samples[2],
          reading.protocol.replace("standing-v1 / ", "서서 촬영 · ").replace(" / ", " · ")), 14, false);
      for (int j = i + 1; j < readings.size(); j++) {
        if (reading.protocol.equals(readings.get(j).protocol)) {
          CoachUi.text(entry, String.format(Locale.KOREA, "같은 조건의 이전 기록 대비 %+.1f°",
              reading.mean - readings.get(j).mean), 16, false);
          break;
        }
      }
      CoachUi.button(entry, "이 측정 삭제", android.R.drawable.ic_menu_delete, () ->
          new AlertDialog.Builder(this).setMessage("이 측정값을 삭제할까요?")
              .setPositiveButton("삭제", (dialog, which) -> {
                try { store.deleteReading(reading.id); history(); }
                catch (RuntimeException e) { error("삭제하지 못했습니다."); }
              }).setNegativeButton("취소", null).show());
    }
    CoachUi.text(body, "각도 차이에는 촬영 자세와 기준점 오차가 포함됩니다. 수치만으로 개선이나 치료 효과를 확정하지 않습니다.", 14, false);
  }

  private void settings() {
    pauseTimer();
    state.screen = "settings";
    LinearLayout body = CoachUi.page(this, "설정 · 데이터");
    CoachUi.button(body, "새 목표 설정", android.R.drawable.ic_menu_edit, this::setup);
    CoachUi.button(body, "나의 실천 약속", android.R.drawable.ic_menu_my_calendar, this::habitCue);
    CoachUi.text(body, "사진은 메모리에서만 처리됩니다. 측정값과 실천 날짜는 이 기기에 보관됩니다.", 16, false);
    CoachUi.text(body, "귀 후보는 온디바이스 ML Kit이 찾습니다. C7과 귀 위치는 직접 확인합니다. 루틴 기간은 사용자가 정하는 습관 목표이며 AI가 회복 기간을 예측한 값이 아닙니다.", 16, false);
    CoachUi.button(body, "측정 방법 근거", android.R.drawable.ic_menu_info_details, () -> {
      try { startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse("https://pmc.ncbi.nlm.nih.gov/articles/PMC7559098/"))); }
      catch (android.content.ActivityNotFoundException e) { error("브라우저를 찾지 못했습니다."); }
    });
    CoachUi.button(body, "운동 안내", android.R.drawable.ic_menu_view, this::exercises);
    CoachUi.button(body, "개인정보·이용 안내", android.R.drawable.ic_menu_info_details,
        () -> startActivity(new Intent(this, PrivacyActivity.class)));
    CoachUi.text(body, "목log\n계정 없이, 기록은 이 기기에", 14, false);
    CoachUi.button(body, "기록 전체 삭제", android.R.drawable.ic_menu_delete, () ->
        new AlertDialog.Builder(this).setTitle("모든 목표의 실천·측정 기록 삭제")
            .setMessage("삭제한 기록은 복구할 수 없습니다. 현재 목표 설정은 유지됩니다.")
            .setPositiveButton("삭제", (dialog, which) -> {
              try { store.clearAll(); home(); }
              catch (RuntimeException e) { error("기록을 삭제하지 못했습니다."); }
            }).setNegativeButton("취소", null).show());
    CoachUi.navigation(body, 2, this::home, this::history, this::settings);
  }

  private void pauseTimer() {
    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    if (timer != null) { timer.cancel(); timer = null; }
    state.routineRunning = false;
    if (artwork != null) artwork.playing(false);
    if (play != null) {
      play.setText("계속하기");
      CoachUi.buttonIcon(play, android.R.drawable.ic_media_play);
    }
  }

  private void editPractice(PostureStore.Practice practice) {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    int pad = CoachUi.dp(this, 20); form.setPadding(pad, pad, pad, pad);
    android.widget.EditText input = CoachUi.input(form, practice.note, "기억할 점을 남겨 주세요 (280자 이내)", 280);
    new AlertDialog.Builder(this).setTitle(practice.day + " 한 줄 기록").setView(form)
        .setPositiveButton("저장", (dialog, which) -> {
          try { store.updatePracticeNote(practice, input.getText().toString()); history(); }
          catch (RuntimeException e) { error("한 줄 기록을 저장하지 못했습니다."); }
        }).setNegativeButton("취소", null).show();
  }

  private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
  @Override protected void onPause() {
    if (state.screen.equals("confirm-practice") && practiceDraft != null) {
      state.practiceNote = practiceDraft.getText().toString();
    }
    pauseTimer(); super.onPause();
  }
  @Override protected void onDestroy() { store.close(); super.onDestroy(); }
  private void navigateBack() {
    if (state.screen.equals("welcome") || state.screen.equals("home") || (!store.hasPlan() && state.screen.equals("setup"))) {
      finish();
    } else home();
  }
}
