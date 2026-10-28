package com.google.mlkit.vision.demo.java.neckposture;

import java.util.Arrays;
import java.util.Collections;

public final class HabitProgressCheck {
  private static int checks;
  public static void main(String[] args) {
    same("2026-10-26", HabitProgress.week("2026-10-28").get(0));
    same("2026-11-01", HabitProgress.week("2026-10-28").get(6));
    same("2026-10-26", HabitProgress.week("2026-11-01").get(0));
    same("2026-11-02", HabitProgress.week("2026-11-02").get(0));
    same("2026-12-28", HabitProgress.week("2027-01-01").get(0));
    same("2024-03-03", HabitProgress.week("2024-02-29").get(6));
    same(2, HabitProgress.completedThisWeek(Arrays.asList("2026-10-26", "2026-10-26",
        "2026-10-27", "2026-11-01"), "2026-10-28"));
    same(0, HabitProgress.completedThisWeek(Arrays.asList("2026-10-25", "2026-10-29"), "2026-10-28"));
    same(1, HabitProgress.completedThisWeek(Collections.singletonList("2026-10-28"), "2026-10-28"));
    same(0, HabitProgress.completedThisWeek(Collections.emptyList(), "2026-10-28"));
    rejects("2026-02-30");
    rejects("2026-10-28junk");
    System.out.println("HabitProgress: " + checks + " checks passed");
  }
  private static void same(Object expected, Object actual) {
    if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    checks++;
  }
  private static void rejects(String date) {
    try { HabitProgress.week(date); throw new AssertionError("Accepted invalid date"); }
    catch (IllegalArgumentException expected) { checks++; }
  }
}
