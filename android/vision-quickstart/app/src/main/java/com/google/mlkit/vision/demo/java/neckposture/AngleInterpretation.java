package com.google.mlkit.vision.demo.java.neckposture;

final class AngleInterpretation {
  static String describe(double[] samples) {
    double mean = CvaMath.mean(samples);
    if (!CvaMath.repeatable(samples)) return "측정값 차이가 커서 해석을 보류합니다. 같은 조건으로 다시 점검해 주세요.";
    double low = Math.min(samples[0], Math.min(samples[1], samples[2]));
    double high = Math.max(samples[0], Math.max(samples[1], samples[2]));
    if (low < 50 && high >= 50) return "세 측정값이 참고 기준 50°의 양쪽에 걸쳐 있습니다. 기준점과 촬영 조건을 다시 확인해 주세요.";
    return mean < 50 ? "평균이 참고 기준 50° 미만입니다. 일부 연구에서 머리가 앞으로 나온 자세를 구분할 때 쓰는 범위입니다."
        : "평균이 참고 기준 50° 이상입니다. 이 수치만으로 정상 자세나 거북목 여부를 확정할 수 없습니다.";
  }
  static final String LIMIT = "연구마다 기준이 다릅니다. 사진 각도는 자가 참고용이며 진단·중증도 판정이 아닙니다.";
  static final String SOURCE = "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0330665";
}
