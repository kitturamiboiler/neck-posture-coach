package com.google.mlkit.vision.demo.java.neckposture;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/** Offline notice available before camera access and from settings. */
public final class PrivacyActivity extends AppCompatActivity {
  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout body = CoachUi.page(this, "개인정보·이용 안내", this::finish);
    CoachUi.text(body, "목log", 24, true);
    CoachUi.text(body, "사진과 기록", 20, true);
    CoachUi.text(body, "카메라 권한은 사진 점검에만 사용합니다. 사진과 얼굴 기준점은 메모리에서 처리하고 사진 파일을 저장하거나 서버로 전송하지 않습니다. 화면을 벗어나면 사진 처리 흐름이 종료됩니다.", 16, false);
    CoachUi.text(body, "기기 안에 남는 정보", 20, true);
    CoachUi.text(body, "목표 기간, 실천 약속, 실천 날짜·루틴 시간·선택한 한 줄 메모, 사진별 각도와 평균, 촬영 조건을 이 기기에 저장합니다. 설정에서 실천·측정 기록을 삭제할 수 있고, 앱 삭제 시 로컬 정보가 삭제됩니다. 계정·광고·자체 서버는 없습니다. 다른 기기로 자동 동기화되지 않습니다.", 16, false);
    CoachUi.text(body, "외부 SDK와 링크", 20, true);
    CoachUi.text(body, "Google ML Kit을 사용해 귀 후보를 찾습니다. ML Kit은 기기·앱 정보와 성능·진단 정보를 처리할 수 있습니다. 사진은 온디바이스에서 분석합니다. 외부 출처 링크는 브라우저에서 열리며 해당 사이트의 개인정보 정책이 적용됩니다.", 16, false);
    CoachUi.text(body, "건강 정보의 한계", 20, true);
    CoachUi.text(body, "목log는 의료기기가 아니며 질환의 진단·치료·예방을 제공하지 않습니다. 각도와 그림은 참고용이고 회복 기간이나 치료 효과를 보장하지 않습니다. 통증·저림·어지럼이 있으면 운동을 중단하고 전문가와 상담하세요.", 16, false);
    CoachUi.text(body, "운동 자료", 20, true);
    CoachUi.text(body, "운동 문구는 Cambridge University Hospitals의 일반 운동 안내를 참고해 요약했습니다. 기관의 영상·사진은 복제하지 않았습니다. 앱의 그림은 자체 제작한 AI 보조 참고 이미지이며 전문가의 실제 시연 영상이 아닙니다. 표시 시간은 휴식이 포함된 습관 목표입니다.", 16, false);
    try (java.io.InputStream input = getAssets().open("licenses.txt");
        java.util.Scanner scanner = new java.util.Scanner(input, "UTF-8")) {
      final String notices = scanner.useDelimiter("\\A").next();
      CoachUi.button(body, "오픈소스·SDK 고지", android.R.drawable.ic_menu_info_details, () ->
          new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("오픈소스·SDK 고지")
              .setMessage(notices).setPositiveButton("닫기", null).show());
    } catch (java.io.IOException ignored) { /* Legacy sample has no release notice asset. */ }
    android.os.Bundle metadata = null;
    try { metadata = getPackageManager().getApplicationInfo(getPackageName(),
        android.content.pm.PackageManager.GET_META_DATA).metaData; }
    catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
    String operator = metadata == null ? "" : metadata.getString("moklog.operator", "");
    String email = metadata == null ? "" : metadata.getString("moklog.email", "");
    String policy = metadata == null ? "" : metadata.getString("moklog.privacy_url", "");
    if (!operator.isEmpty() && !email.isEmpty() && policy.startsWith("https://")) {
      CoachUi.text(body, "문의·공개 방침", 20, true);
      CoachUi.text(body, operator + "\n문의: " + email, 16, false);
      CoachUi.button(body, "공개 개인정보처리방침", android.R.drawable.ic_menu_info_details, () -> {
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(policy))); }
        catch (android.content.ActivityNotFoundException e) {
          android.widget.Toast.makeText(this, "브라우저를 찾지 못했습니다.", android.widget.Toast.LENGTH_LONG).show();
        }
      });
    }
  }
}
