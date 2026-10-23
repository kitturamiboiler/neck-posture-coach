package com.google.mlkit.vision.demo.java.neckposture;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

final class PostureStore extends SQLiteOpenHelper {
  final SharedPreferences prefs;

  PostureStore(Context context) {
    super(context, "posture_habits.db", null, 2);
    prefs = context.getSharedPreferences("posture_plan", Context.MODE_PRIVATE);
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE practice (plan TEXT NOT NULL, day TEXT NOT NULL,"
        + " seconds INTEGER NOT NULL, PRIMARY KEY(plan, day))");
    db.execSQL("CREATE TABLE measurement (_id INTEGER PRIMARY KEY, plan TEXT NOT NULL,"
        + " created INTEGER NOT NULL, protocol TEXT NOT NULL, a REAL NOT NULL,"
        + " b REAL NOT NULL, c REAL NOT NULL, median REAL NOT NULL, mean REAL NOT NULL)");
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1 && newVersion == 2) {
      db.execSQL("ALTER TABLE measurement ADD COLUMN mean REAL");
      db.execSQL("UPDATE measurement SET mean=(a+b+c)/3.0");
    } else throw new IllegalStateException("Unsupported database upgrade");
  }

  static String today() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }

  static int elapsedDays(String start, String end) {
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      format.setLenient(false);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return (int) ((format.parse(end).getTime() - format.parse(start).getTime()) / 86400000L);
    } catch (java.text.ParseException e) {
      throw new IllegalArgumentException("Invalid plan date", e);
    }
  }

  boolean hasPlan() { return prefs.contains("plan"); }
  String plan() { return prefs.getString("plan", ""); }
  String start() { return prefs.getString("start", today()); }
  int weeks() { return prefs.getInt("weeks", 4); }
  int minutes() { return prefs.getInt("minutes", 3); }
  int day() { return Math.max(1, elapsedDays(start(), today()) + 1); }

  void createPlan(int weeks, int minutes) {
    if ((weeks != 2 && weeks != 4) || (minutes != 3 && minutes != 5)) {
      throw new IllegalArgumentException("Invalid habit goal");
    }
    if (!prefs.edit().putString("plan", java.util.UUID.randomUUID().toString())
        .putString("start", today()).putInt("weeks", weeks).putInt("minutes", minutes).commit()) {
      throw new IllegalStateException("Could not save plan");
    }
  }

  boolean practicedToday() {
    try (Cursor cursor = getReadableDatabase().rawQuery(
        "SELECT 1 FROM practice WHERE plan=? AND day=?", new String[]{plan(), today()})) {
      return cursor.moveToFirst();
    }
  }

  void finishPractice(int seconds) {
    if (!hasPlan() || seconds < 1) throw new IllegalArgumentException("Invalid practice");
    ContentValues values = new ContentValues();
    values.put("plan", plan());
    values.put("day", today());
    values.put("seconds", seconds);
    getWritableDatabase().insertWithOnConflict("practice", null, values,
        SQLiteDatabase.CONFLICT_IGNORE);
  }

  List<String> practiceDays() {
    return practiceDays(false);
  }

  List<String> practiceDays(boolean allPlans) {
    List<String> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().rawQuery(
        allPlans ? "SELECT DISTINCT day FROM practice ORDER BY day DESC"
            : "SELECT day FROM practice WHERE plan=? ORDER BY day DESC",
        allPlans ? null : new String[]{plan()})) {
      while (cursor.moveToNext()) result.add(cursor.getString(0));
    }
    return result;
  }

  long saveMeasurement(double[] values, String protocol) {
    if (!hasPlan() || !CvaMath.repeatable(values) || protocol == null || protocol.isEmpty()) {
      throw new IllegalArgumentException("Measurement needs review");
    }
    ContentValues row = new ContentValues();
    row.put("plan", plan());
    row.put("created", System.currentTimeMillis());
    row.put("protocol", protocol);
    row.put("a", values[0]); row.put("b", values[1]); row.put("c", values[2]);
    row.put("median", CvaMath.median(values));
    row.put("mean", CvaMath.mean(values));
    return getWritableDatabase().insertOrThrow("measurement", null, row);
  }

  List<Reading> readings() {
    return readings(false);
  }

  List<Reading> readings(boolean allPlans) {
    List<Reading> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().rawQuery(
        "SELECT _id,created,protocol,a,b,c,mean FROM measurement"
            + (allPlans ? "" : " WHERE plan=?") + " ORDER BY _id DESC",
        allPlans ? null : new String[]{plan()})) {
      while (cursor.moveToNext()) result.add(new Reading(cursor));
    }
    return result;
  }

  void deleteReading(long id) {
    getWritableDatabase().delete("measurement", "_id=?", new String[]{Long.toString(id)});
  }

  void clearAll() {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete("practice", null, null);
      db.delete("measurement", null, null);
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  static final class Reading {
    final long id, created;
    final String protocol;
    final double[] samples;
    final double mean;
    Reading(Cursor row) {
      id = row.getLong(0); created = row.getLong(1); protocol = row.getString(2);
      samples = new double[]{row.getDouble(3), row.getDouble(4), row.getDouble(5)};
      mean = row.getDouble(6);
    }
  }
}
