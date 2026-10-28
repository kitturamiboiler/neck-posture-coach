package com.google.mlkit.vision.demo.java.neckposture;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Calendar dates, independent of clock changes and daylight-saving offsets. */
final class HabitProgress {
  private HabitProgress() {}

  static List<String> week(String today) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    format.setLenient(false);
    ParsePosition position = new ParsePosition(0);
    java.util.Date date = format.parse(today, position);
    if (date == null || position.getIndex() != today.length() || !format.format(date).equals(today)) {
      throw new IllegalArgumentException("Invalid calendar date");
    }
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
    calendar.setTime(date);
    calendar.add(Calendar.DATE, -(calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7);
    List<String> result = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      result.add(format.format(calendar.getTime()));
      calendar.add(Calendar.DATE, 1);
    }
    return result;
  }

  static int completedThisWeek(Collection<String> completed, String today) {
    HashSet<String> dates = new HashSet<>(completed);
    int count = 0;
    for (String date : week(today)) if (date.compareTo(today) <= 0 && dates.contains(date)) count++;
    return count;
  }
}
