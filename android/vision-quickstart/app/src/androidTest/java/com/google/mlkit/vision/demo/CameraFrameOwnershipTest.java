package com.google.mlkit.vision.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CameraFrameOwnershipTest {
  @Test
  public void recyclingCameraArrayCannotChangeProcessingOrPendingFrames() {
    byte[] cameraBytes = new byte[640 * 480 * 3 / 2];
    Arrays.fill(cameraBytes, (byte) 10);
    ByteBuffer cameraBuffer = ByteBuffer.wrap(cameraBytes);
    ByteBuffer processing = CameraSource.copyFrame(cameraBuffer);
    Arrays.fill(cameraBytes, (byte) 20);
    ByteBuffer pending = CameraSource.copyFrame(cameraBuffer);
    Arrays.fill(cameraBytes, (byte) 30);

    assertNotSame(cameraBytes, processing.array());
    assertNotSame(processing.array(), pending.array());
    assertEquals(0, processing.position());
    assertEquals(cameraBytes.length, processing.remaining());
    assertEquals(10, processing.get(0));
    assertEquals(10, processing.get(cameraBytes.length - 1));
    assertEquals(20, pending.get(0));
    assertEquals(20, pending.get(cameraBytes.length - 1));
  }
}
