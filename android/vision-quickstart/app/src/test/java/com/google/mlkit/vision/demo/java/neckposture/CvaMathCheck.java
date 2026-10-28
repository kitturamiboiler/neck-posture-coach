package com.google.mlkit.vision.demo.java.neckposture;

/** Run with javac/java; no Android device or test dependency required. */
public final class CvaMathCheck {
  public static void main(String[] args) {
    check(Math.abs(CvaMath.angle(100, 0, 0, 100) - 45) < 0.0001, "45 degrees");
    check(CvaMath.angle(0, 0, 0, 100) == 90, "vertical");
    check(CvaMath.angle(-100, 0, 0, 100) == CvaMath.angle(100, 0, 0, 100), "mirrored");
    check(CvaMath.angle(200, 10, 100, 110) == 45, "translation");
    check(CvaMath.angle(200, 0, 0, 200) == 45, "uniform scale");
    check(Double.isNaN(CvaMath.angle(Double.NaN, 0, 0, 100)), "NaN coordinate");
    check(Double.isNaN(CvaMath.angle(0, 0, 0, Double.POSITIVE_INFINITY)), "infinite coordinate");
    check(Double.isNaN(CvaMath.angle(0, 100, 0, 0)), "ear below base");
    check(Double.isNaN(CvaMath.angle(0, 0, 0, 0)), "coincident points");
    check(Double.isNaN(CvaMath.angle(0, 0, 0, 0.2)), "too close");
    double[] values = {52, 49, 50};
    check(CvaMath.median(values) == 50 && values[0] == 52, "median without mutation");
    check(Math.abs(CvaMath.mean(values) - 50.3333333333) < .00001 && values[0] == 52, "mean without mutation");
    check(Math.abs(CvaMath.mean(new double[]{52.5, 55.4, 55.7}) - 54.5333333333) < .00001, "decimal mean");
    check(CvaMath.repeatable(values), "consistent samples");
    check(CvaMath.repeatable(new double[]{50, 55, 51}), "five degree boundary");
    check(!CvaMath.repeatable(new double[]{45, 55, 51}), "variable samples blocked");
    for (double[] invalid : new double[][]{{1, 2}, {0, 50, 50}, {91, 50, 50}, {Double.NaN, 50, 50}}) {
      boolean rejected = false;
      try { CvaMath.median(invalid); } catch (IllegalArgumentException e) { rejected = true; }
      check(rejected, "invalid sample rejected");
    }
    for (double[] invalid : new double[][]{null, {1, 2}, {0, 50, 50}, {91, 50, 50}, {Double.NaN, 50, 50}, {Double.POSITIVE_INFINITY, 50, 50}}) {
      boolean rejected = false;
      try { CvaMath.mean(invalid); } catch (IllegalArgumentException e) { rejected = true; }
      check(rejected, "invalid mean rejected");
    }
    System.out.println("CVA checks passed (26 checks)");
    check(AngleInterpretation.describe(new double[]{48,49,49}).contains("미만"), "below reference");
    check(AngleInterpretation.describe(new double[]{50,50,50}).contains("이상"), "reference equality");
    check(AngleInterpretation.describe(new double[]{49,50,51}).contains("양쪽"), "straddling reference");
    check(AngleInterpretation.describe(new double[]{40,50,60}).contains("보류"), "quality blocks interpretation");
    check(ExerciseLibrary.stage(180,180) == 0 && ExerciseLibrary.stage(90,180) == 1
        && ExerciseLibrary.stage(0,180) == 1, "routine phases");
    System.out.println("Interpretation and exercise checks passed (5 checks)");
  }

  private static void check(boolean result, String description) {
    if (!result) throw new AssertionError(description);
  }
}
