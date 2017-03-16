package com.linkedin.util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFastByteArrayOutputStream {
  @Test
  public void testEmpty()
  {
    FastByteArrayOutputStream testStream = new FastByteArrayOutputStream();
    Assert.assertEquals(testStream.size(), 0);
    Assert.assertEquals(testStream.toByteArray().length, 0);
  }

  @Test
  public void testWrite() throws Exception
  {
    FastByteArrayOutputStream testStream1 = new FastByteArrayOutputStream();
    testStream1.write(1);
    Assert.assertEquals(testStream1.size(), 1);
    Assert.assertEquals(testStream1.toByteArray().length, 1);
    Assert.assertEquals(testStream1.toByteArray()[0], 0b1);

    FastByteArrayOutputStream testStream2 = new FastByteArrayOutputStream();
    byte[] inputArray = new byte[] {0b1, 0b1, 0b1, 0b1, 0b0, 0b0, 0b0, 0b0};
    testStream2.write(inputArray, 0, inputArray.length);
    Assert.assertEquals(testStream2.size(), inputArray.length);
    Assert.assertEquals(testStream2.toByteArray(), inputArray);

    FastByteArrayOutputStream testStream3 = new FastByteArrayOutputStream();
    Field maxSizeField = FastByteArrayOutputStream.class.getDeclaredField("MAX_STREAM_SIZE");
    maxSizeField.setAccessible(true);
    int maxSize = (int) maxSizeField.get(null);
    byte[] maxArray = new byte[0];  // Don't allocate a real array with maxSize to avoid OOM in test environment.
    Assert.assertThrows(IndexOutOfBoundsException.class, () -> testStream3.write(maxArray, 0, maxSize + 1));
  }

  @Test
  public void testToByteArray()
  {
    FastByteArrayOutputStream testStream = new FastByteArrayOutputStream();
    byte[] inputArray = new byte[] {0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1};
    testStream.write(inputArray, 0, inputArray.length);

    byte[] safeArray1 = testStream.toByteArray();
    byte[] safeArray2 = testStream.toByteArray();

    Assert.assertEquals(testStream.size(), inputArray.length);
    Assert.assertEquals(safeArray1, inputArray);
    Assert.assertEquals(safeArray2, inputArray);

    // The two byte array has the same content but different identities.
    Assert.assertEquals(safeArray1, safeArray2);
    Assert.assertNotSame(safeArray1, safeArray2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddBuffer() throws Exception
  {
    FastByteArrayOutputStream testStream = new FastByteArrayOutputStream();
    Field bufferListField = testStream.getClass().getDeclaredField("_bufferList");
    bufferListField.setAccessible(true);
    // Empty linked list until the first write.
    Assert.assertEquals(((LinkedList<byte[]>) bufferListField.get(testStream)).size(), 0);

    testStream.write(1);
    Assert.assertEquals(((LinkedList<byte[]>) bufferListField.get(testStream)).size(), 1);

    Field defaultSizeField = FastByteArrayOutputStream.class.getDeclaredField("DEFAULT_BUFFER_SIZE");
    defaultSizeField.setAccessible(true);
    int defaultSize = (int) defaultSizeField.get(null);

    byte[] testArray = new byte[defaultSize];
    testStream.write(testArray, 0, testArray.length);
    // Exceed the capacity of DEFAULT_BUFFER_SIZE and the second buffer is added.
    Assert.assertEquals(((LinkedList<byte[]>) bufferListField.get(testStream)).size(), 2);
    Assert.assertEquals(testStream.toByteArray().length, defaultSize + 1);
  }
}
