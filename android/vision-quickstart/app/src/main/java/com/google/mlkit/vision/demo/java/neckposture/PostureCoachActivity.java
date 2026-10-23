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
  private static final String EXERCISE_SOURCE =
      "https://www.cuh.nhs.uk/patient-information/neck-exercises-and-advice/";

  @Override protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    getWindow().setStatusBarColor(CoachUi.GREEN);
    store = new PostureStore(this);
    state = new ViewModelProvider(this).get(PostureSession.class);
  }

  @Override protected void onResume() {
    super.onResume();
    if (!store.hasPlan()) setup();
    else if (state.screen.equals("routine")) routine();
    else if (state.screen.equals("history")) history();
    else if (state.screen.equals("settings")) settings();
    else home();
  }

  private void setup() {
    state.screen = "setup";
    LinearLayout body = CoachUi.page(this, "목 자세 코치");
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
        home();
      } catch (RuntimeException e) { error("목표를 저장하지 못했습니다. 다시 시도해 주세요."); }
    }), true);
    if (store.hasPlan()) CoachUi.button(body, "돌아가기", android.R.drawable.ic_menu_revert, this::home);
  }

  private void home() {
    pauseTimer();
    state.screen = "home";
    LinearLayout body = CoachUi.page(this, "오늘");
    int total = store.weeks() * 7;
    int day = store.day();
    CoachUi.text(body, "오늘도 가볍게,\n꾸준히 실천해요.", 24, true);
    LinearLayout goal = CoachUi.card(body, true);
    CoachUi.text(goal, day > total ? "목표 기간을 마쳤어요" : "나의 목표 · " + day + "일차", 14, false);
    CoachUi.text(goal, store.weeks() + "주 동안 하루 " + store.minutes() + "분", 20, true);
    ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(total);
    progress.setProgress(Math.min(day, total));
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CoachUi.GREEN));
    goal.addView(progress, new LinearLayout.LayoutParams(-1, CoachUi.dp(this, 8)));
    List<String> days = store.practiceDays();
    CoachUi.text(goal, "실천 " + days.size() + "일 / 목표 " + total + "일", 14, false);
    LinearLayout practice = CoachUi.card(body, false);
    boolean done = store.practicedToday();
    CoachUi.text(practice, "오늘의 실천", 20, true);
    CoachUi.text(practice, done ? "오늘 운동을 기록했어요" : "오늘 운동을 아직 기록하지 않았어요", 14, false);
    CoachUi.styleButton(CoachUi.button(practice, done ? "루틴 다시 보기" : "운동 시작",
        android.R.drawable.ic_media_play, this::routine), true);
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
    CoachUi.text(body, checkpoint, 14, false);
    if (!readings.isEmpty()) {
      CoachUi.text(body, String.format(Locale.KOREA, "최근 평균 CVA %.1f°", readings.get(0).mean), 24, true);
      CoachUi.text(body, "직접 확인한 귀·C7 기준의 참고값", 14, false);
    }
    CoachUi.button(body, "사진 점검", android.R.drawable.ic_menu_camera,
        () -> startActivity(new Intent(this, PostureCaptureActivity.class)));
    CoachUi.button(body, "지난 기록 보기", android.R.drawable.ic_menu_recent_history, this::history);
    if (day > total) CoachUi.button(body, "다음 목표 설정", android.R.drawable.ic_menu_edit, this::setup);
    CoachUi.navigation(body, 0, this::home, this::history, this::settings);
  }

  private void routine() {
    pauseTimer();
    state.screen = "routine";
    if (state.routineRemaining <= 0) state.routineRemaining = store.minutes() * 60;
    LinearLayout body = CoachUi.page(this, "오늘의 운동", this::home);
    CoachUi.text(body, "일반 운동 안내 · 편안한 범위에서", 15, false);
    CoachUi.text(body, "통증·어지럼·저림이 생기면 중단하세요. 최근 다쳤거나 치료 중이면 담당 전문가에게 먼저 확인하세요.", 15, false);
    movement = CoachUi.text(body, "", 22, true);
    movementDetail = CoachUi.text(body, "", 17, false);
    LinearLayout media = CoachUi.card(body, true);
    TextView pending = CoachUi.text(media, "운동 자료 준비 중", 16, true);
    pending.setGravity(android.view.Gravity.CENTER);
    pending.setMinHeight(CoachUi.dp(this, 120));
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
    boolean chin = remaining > store.minutes() * 30;
    movement.setText(chin ? "1. 가볍게 턱 당기기" : "2. 편안한 목 돌리기");
    movementDetail.setText(chin
        ? "등과 목을 곧게 유지하고 턱을 가볍게 뒤로 당겼다가 편히 돌아옵니다. 고개를 아래로 숙이지 않습니다."
        : "어깨의 힘을 빼고 편안한 범위에서 고개를 좌우로 천천히 돌립니다. 손으로 머리를 밀거나 당기지 않습니다.");
    play.setText(state.routineRunning ? "일시정지" :
        state.routineRemaining == store.minutes() * 60 ? "시작" : "계속하기");
    CoachUi.buttonIcon(play, state.routineRunning ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
  }

  private void startTimer() {
    state.routineRunning = true;
    refreshRoutine();
    timer = new CountDownTimer(state.routineRemaining * 1000L, 250) {
      @Override public void onTick(long millis) {
        state.routineRemaining = (int) Math.ceil(millis / 1000.0);
        refreshRoutine();
      }
      @Override public void onFinish() {
        state.routineRemaining = 0;
        state.routineRunning = false;
        timer = null;
        confirmPractice();
      }
    }.start();
  }

  private void confirmPractice() {
    new AlertDialog.Builder(this).setTitle("오늘의 루틴을 마쳤나요?")
        .setMessage("실제로 수행한 경우에만 오늘의 실천으로 기록합니다.")
        .setPositiveButton("완료 기록", (dialog, which) -> {
          try { store.finishPractice(store.minutes() * 60); home(); }
          catch (RuntimeException e) { error("기록을 저장하지 못했습니다."); confirmPractice(); }
        }).setNegativeButton("기록하지 않기", (dialog, which) -> home())
        .setCancelable(false).show();
  }

  private void history() {
    pauseTimer();
    state.screen = "history";
    LinearLayout body = CoachUi.page(this, "내 기록");
    CoachUi.text(body, "모든 목표의 실천·측정 기록", 15, false);
    CoachUi.text(body, "대표 각도는 사진 3장의 평균입니다. 이전 기록도 원래의 사진별 값으로 평균을 계산해 표시합니다.", 14, false);
    CoachUi.text(body, "운동 실천", 20, true);
    List<String> days = store.practiceDays(true);
    CoachUi.text(body, days.isEmpty() ? "아직 실천 기록이 없어요." : android.text.TextUtils.join("   ·   ", days), 16, false);
    CoachUi.rule(body);
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
    CoachUi.navigation(body, 1, this::home, this::history, this::settings);
  }

  private void settings() {
    pauseTimer();
    state.screen = "settings";
    LinearLayout body = CoachUi.page(this, "설정 · 데이터");
    CoachUi.text(body, "사진은 메모리에서만 처리됩니다. 측정값과 실천 날짜는 이 기기에 보관됩니다.", 16, false);
    CoachUi.text(body, "귀 후보는 온디바이스 ML Kit이 찾습니다. C7과 귀 위치는 직접 확인합니다. 루틴 기간은 사용자가 정하는 습관 목표이며 AI가 회복 기간을 예측한 값이 아닙니다.", 16, false);
    CoachUi.button(body, "측정 방법 근거", android.R.drawable.ic_menu_info_details, () -> {
      try { startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse("https://pmc.ncbi.nlm.nih.gov/articles/PMC7559098/"))); }
      catch (android.content.ActivityNotFoundException e) { error("브라우저를 찾지 못했습니다."); }
    });
    CoachUi.button(body, "실험용 ML Kit 화면", android.R.drawable.ic_menu_manage,
        () -> startActivity(new Intent(this, com.google.mlkit.vision.demo.java.ChooserActivity.class)));
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
    if (timer != null) { timer.cancel(); timer = null; }
    state.routineRunning = false;
    if (play != null) {
      play.setText("계속하기");
      CoachUi.buttonIcon(play, android.R.drawable.ic_media_play);
    }
  }

  private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
  @Override protected void onPause() { pauseTimer(); super.onPause(); }
  @Override protected void onDestroy() { store.close(); super.onDestroy(); }
  @Override public void onBackPressed() {
    if (state.screen.equals("home") || (!store.hasPlan() && state.screen.equals("setup"))) {
      super.onBackPressed();
    } else home();
  }
}
