package com.google.mlkit.vision.demo.java.neckposture;

/** General movement education, never an angle-based prescription. */
final class ExerciseLibrary {
  static final String SOURCE = "https://www.cuh.nhs.uk/patient-information/neck-exercises-and-advice/";
  static final String SAFETY = "통증·어지럼·저림이 생기면 중단하세요. 최근 다쳤거나 치료 중이면 담당 전문가에게 먼저 확인하세요.";
  static final String[] TITLES = {"가볍게 턱 당기기", "편안하게 목 돌리기"};
  static final String[] STEPS = {
      "1. 등을 편안히 세우고 정면을 봅니다.\n2. 시선 높이를 유지하며 턱을 가볍게 뒤로 당깁니다.\n3. 힘을 풀고 편안한 위치로 돌아옵니다.",
      "1. 어깨의 힘을 빼고 정면을 봅니다.\n2. 고개를 편안한 범위에서 천천히 한쪽으로 돌립니다.\n3. 정면으로 돌아온 뒤 반대쪽도 천천히 움직입니다."
  };
  static final String[] CAUTIONS = {
      "고개를 아래로 숙이거나 턱을 위로 들지 않습니다. 강하게 밀거나 오래 버티지 마세요.",
      "손으로 머리를 밀거나 당기지 않습니다. 아픈 끝 범위까지 돌리지 마세요."
  };
  static int stage(int remaining, int total) {
    if (remaining < 0 || total <= 0 || remaining > total) throw new IllegalArgumentException("Invalid duration");
    return remaining > total / 2 ? 0 : 1;
  }
}
