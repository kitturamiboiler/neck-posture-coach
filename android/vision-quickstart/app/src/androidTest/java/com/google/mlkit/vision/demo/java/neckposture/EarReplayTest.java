package com.google.mlkit.vision.demo.java.neckposture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Optional local replay. The user's screenshot is not bundled or uploaded. */
@RunWith(AndroidJUnit4.class)
public class EarReplayTest {
  @Test
  public void replaysReportedProfileAndItsReflection() throws Exception {
    File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
        .getExternalFilesDir(null), "ear-regression.png");
    assumeTrue("Supply the reported full screenshot locally before replay", file.isFile());
    Bitmap source = BitmapFactory.decodeFile(file.getPath());
    FaceDetector detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.1f).build());
    try {
      for (boolean mirrored : new boolean[] {false, true}) {
        Matrix matrix = new Matrix();
        matrix.setScale(mirrored ? -1 : 1, 1);
        Bitmap input = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(), source.getHeight(), matrix, false);
        List<Face> faces = Tasks.await(detector.process(InputImage.fromBitmap(input, 0)),
            20, TimeUnit.SECONDS);
        assertFalse("Face missing in supplied regression image", faces.isEmpty());
        Face face = NeckPostureProcessor.largestFace(faces);
        int side = NeckPostureProcessor.sideForYaw(face.getHeadEulerAngleY());
        FaceLandmark selected = face.getLandmark(NeckPostureProcessor.faceEarTypeForPoseSide(side));
        assertNotNull(selected);
        PointF pointInOriginal = new PointF(
            mirrored ? source.getWidth() - selected.getPosition().x : selected.getPosition().x,
            selected.getPosition().y);
        // Manually identified visible ear region in the supplied 2560x1600 screenshot.
        // This tests ear selection, not subpixel ear-canal accuracy or a clinical measurement.
        RectF visibleEar = new RectF(source.getWidth() * 0.25f, source.getHeight() * 0.31f,
            source.getWidth() * 0.29f, source.getHeight() * 0.42f);
        assertTrue("Selected point is outside the visible ear: " + pointInOriginal,
            visibleEar.contains(pointInOriginal.x, pointInOriginal.y));
        FaceLandmark oldSelection = face.getLandmark(
            side < 0 ? FaceLandmark.LEFT_EAR : FaceLandmark.RIGHT_EAR);
        assertNotNull(oldSelection);
        float oldX = mirrored ? source.getWidth() - oldSelection.getPosition().x
            : oldSelection.getPosition().x;
        assertFalse("Regression image must reproduce the old wrong-side selection",
            visibleEar.contains(oldX, oldSelection.getPosition().y));
        Log.i("EarReplay", "mirrored=" + mirrored + " size=" + input.getWidth() + "x"
            + input.getHeight() + " yaw=" + face.getHeadEulerAngleY());
        for (int type : new int[] {FaceLandmark.LEFT_EAR, FaceLandmark.RIGHT_EAR,
            FaceLandmark.NOSE_BASE, FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE}) {
          FaceLandmark point = face.getLandmark(type);
          Log.i("EarReplay", "type=" + type + " point="
              + (point == null ? "missing" : point.getPosition()));
        }
        if (input != source) {
          input.recycle();
        }
      }
    } finally {
      detector.close();
      source.recycle();
    }
  }
}
