package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PostureFlowTest {
  @Test public void recordsSurviveReopenAndInvalidComparisonsAreNotSaved() {
    // Instrumentation runs as the app UID; isolate test storage by name, not APK context.
    Context context = new ContextWrapper(
        InstrumentationRegistry.getInstrumentation().getTargetContext()) {
      @Override public java.io.File getDatabasePath(String name) {
        return super.getDatabasePath("posture_test_" + name);
      }
      @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
          SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return super.openOrCreateDatabase("posture_test_" + name, mode, factory, errorHandler);
      }
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences("posture_test_" + name, mode);
      }
    };
    try (PostureStore store = new PostureStore(context)) {
      assertEquals(context.getDatabasePath("posture_habits.db").getAbsolutePath(),
          store.getWritableDatabase().getPath());
      store.clearAll();
      store.createPlan(4, 3);
      store.finishPractice(180);
      store.finishPractice(180);
      assertEquals(1, store.practiceDays().size());
      assertTrue(store.practicedToday());
      store.saveMeasurement(new double[]{50, 51, 54}, "standing-v1 / left / back");
      try {
        store.saveMeasurement(new double[]{30, 50, 60}, "standing-v1 / left / back");
        fail("Unstable measurement should not be stored");
      } catch (IllegalArgumentException expected) { }
    }
    try (PostureStore reopened = new PostureStore(context)) {
      assertEquals(155.0 / 3, reopened.readings().get(0).mean, 0.0001);
      reopened.createPlan(2, 5);
      assertEquals(0, reopened.readings().size());
      assertEquals(1, reopened.readings(true).size());
      assertEquals(1, reopened.practiceDays(true).size());
      reopened.clearAll();
    }
    assertEquals(1, PostureStore.elapsedDays("2026-03-08", "2026-03-09"));
    assertEquals(2, PostureStore.elapsedDays("2024-02-28", "2024-03-01"));
  }

  @Test public void photoTouchUsesImageCoordinatesDespiteLetterboxing() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
      android.widget.FrameLayout parent = new android.widget.FrameLayout(context);
      PosturePhotoView view = new PosturePhotoView(context);
      parent.addView(view);
      Bitmap bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888);
      bitmap.setDensity(640);
      view.photo(bitmap, null, null);
      view.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
      view.layout(0, 0, 400, 400);
      MotionEvent inside = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150, 100, 0);
      view.onTouchEvent(inside);
      inside.recycle();
      assertEquals(50, view.ear.x, 0.01);
      assertEquals(100, view.ear.y, 0.01);
      MotionEvent outside = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 30, 100, 0);
      view.onTouchEvent(outside);
      outside.recycle();
      assertEquals(50, view.ear.x, 0.01);
      view.editingEar = false;
      view.base = new PointF(0, 150);
      assertEquals(45, view.angle(), 0.01);
      view.nudge(0, 10);
      assertTrue(view.angle() > 45);
    });
  }

  @Test public void versionOneUpgradePreservesSamplesMedianPracticeAndPlan() {
    Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
    base.deleteDatabase("posture_migration_test.db");
    Context context = new ContextWrapper(base) {
      @Override public java.io.File getDatabasePath(String name) {
        return super.getDatabasePath("posture_migration_test.db");
      }
      @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
          SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
        return super.openOrCreateDatabase("posture_migration_test.db", mode, factory, handler);
      }
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences("posture_migration_test_plan", mode);
      }
    };
    assertNotEquals(base.getDatabasePath("posture_habits.db"), context.getDatabasePath("posture_habits.db"));
    context.getSharedPreferences("posture_plan", Context.MODE_PRIVATE).edit().clear()
        .putString("plan", "old-plan").putInt("weeks", 4).putInt("minutes", 3).commit();
    try (SQLiteDatabase old = context.openOrCreateDatabase("posture_habits.db", 0, null, null)) {
      old.execSQL("CREATE TABLE practice (plan TEXT NOT NULL, day TEXT NOT NULL, seconds INTEGER NOT NULL, PRIMARY KEY(plan,day))");
      old.execSQL("CREATE TABLE measurement (_id INTEGER PRIMARY KEY, plan TEXT NOT NULL, created INTEGER NOT NULL, protocol TEXT NOT NULL, a REAL NOT NULL,b REAL NOT NULL,c REAL NOT NULL,median REAL NOT NULL)");
      old.execSQL("INSERT INTO practice VALUES ('old-plan','2026-10-01',180)");
      old.execSQL("INSERT INTO measurement VALUES (7,'old-plan',1234,'standing-v1 / left / back',52,49,50,50)");
      old.setVersion(1);
    }
    try (PostureStore upgraded = new PostureStore(context)) {
      assertEquals(context.getDatabasePath("posture_habits.db").getAbsolutePath(),
          upgraded.getWritableDatabase().getPath());
      PostureStore.Reading reading = upgraded.readings().get(0);
      assertEquals(7, reading.id);
      assertEquals(1234, reading.created);
      assertArrayEquals(new double[]{52,49,50}, reading.samples, .00001);
      assertEquals(151.0 / 3, reading.mean, .00001);
      assertEquals(1, upgraded.practiceDays().size());
      assertEquals("old-plan", upgraded.plan());
      try (android.database.Cursor cursor = upgraded.getReadableDatabase().rawQuery("SELECT median FROM measurement WHERE _id=7", null)) {
        assertTrue(cursor.moveToFirst());
        assertEquals(50, cursor.getDouble(0), .00001);
      }
      upgraded.saveMeasurement(new double[]{52.5,55.4,55.7}, "standing-v1 / left / back");
    }
    try (PostureStore reopened = new PostureStore(context)) {
      assertEquals(2, reopened.readings().size());
      assertEquals(163.6 / 3, reopened.readings().get(0).mean, .00001);
      assertEquals(2, reopened.getReadableDatabase().getVersion());
    }
    base.deleteDatabase("posture_migration_test.db");
  }
}
