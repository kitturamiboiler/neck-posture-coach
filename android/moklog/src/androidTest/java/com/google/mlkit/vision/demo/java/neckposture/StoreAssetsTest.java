package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.mlkit.vision.demo.R;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Renders supplied branding through Android, without using a person's camera image. */
@RunWith(AndroidJUnit4.class)
public class StoreAssetsTest {
  @Test public void renderStoreBranding() throws Exception {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File directory = new File(context.getCacheDir(), "store-assets");
    assertTrue(directory.isDirectory() || directory.mkdirs());
    Drawable logo = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, R.drawable.moklog_logo);
    assertNotNull(logo);
    Bitmap icon = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(icon);
    canvas.drawColor(0xff084f3d);
    logo.setBounds(0, 0, 512, 512);
    logo.draw(canvas);
    save(icon, new File(directory, "play-icon-512.png"));
    icon.recycle();
    Bitmap feature = Bitmap.createBitmap(1024, 500, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(feature);
    canvas.drawColor(0xff084f3d);
    logo.setBounds(628, 72, 984, 428);
    logo.draw(canvas);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(0xffffffff);
    paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    paint.setTextSize(96);
    canvas.drawText("목log", 48, 192, paint);
    paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    paint.setTextSize(30);
    canvas.drawText("매일의 작은 실천,", 48, 284, paint);
    canvas.drawText("기록으로 이어가요.", 48, 332, paint);
    save(feature, new File(directory, "feature-graphic-1024x500.png"));
    feature.recycle();
  }
  @Test public void productionEarMappingMatchesPinnedVersion() {
    assertEquals(0, FaceLandmarkMapping.sideForYaw(0));
    assertEquals(0, FaceLandmarkMapping.sideForYaw(Float.NaN));
    assertEquals(-1, FaceLandmarkMapping.sideForYaw(-45));
    assertEquals(1, FaceLandmarkMapping.sideForYaw(45));
    assertEquals(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EAR, FaceLandmarkMapping.earForSide(-1));
    assertEquals(com.google.mlkit.vision.face.FaceLandmark.LEFT_EAR, FaceLandmarkMapping.earForSide(1));
  }
  private static void save(Bitmap bitmap, File output) throws Exception {
    try (FileOutputStream stream = new FileOutputStream(output)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
  }
}
