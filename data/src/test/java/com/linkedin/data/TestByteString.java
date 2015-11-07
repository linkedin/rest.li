/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* $Id$ */
package com.linkedin.data;


import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestByteString
{
  @Test
  public void testEmpty()
  {
    Assert.assertSame(ByteString.empty(), ByteString.empty());
    Assert.assertEquals(ByteString.empty(), ByteString.empty());
    Assert.assertEquals(ByteString.empty().hashCode(), ByteString.empty().hashCode());

    Assert.assertEquals(new byte[0], ByteString.empty().copyBytes());
    Assert.assertEquals("", ByteString.empty().asString("UTF-8"));

    final ByteBuffer byteBuffer = ByteString.empty().asByteBuffer();
    Assert.assertEquals(0, byteBuffer.remaining());
    Assert.assertEquals(0, byteBuffer.capacity());
    Assert.assertTrue(byteBuffer.isReadOnly());

    ByteString twoEmpties = cons(ByteString.empty(), ByteString.empty());
    Assert.assertSame(twoEmpties, ByteString.empty());
  }

  @Test
  public void testZeroBytes()
  {
    Assert.assertSame(ByteString.empty(), ByteString.copy(new byte[0]));
  }

  @Test
  public void testCopy()
  {
    final byte[] bytes = new byte[] {1,2,3,4,5};
    final ByteString bs = ByteString.copy(bytes);
    Assert.assertEquals(bytes, bs.copyBytes());

    final byte[] region = new byte[] {2,3,4};
    final ByteString bsRegion = ByteString.copy(bytes, 1, 3);
    Assert.assertEquals(region, bsRegion.copyBytes());
  }

  @Test
  public void testCopyString()
  {
    final String str = "test string";
    final ByteString bs = ByteString.copyString(str, "UTF-8");
    Assert.assertEquals("test string", bs.asString("UTF-8"));
  }

  @Test
  public void testReadKnownLength() throws IOException, InterruptedException, TimeoutException, ExecutionException
  {
    testRead(true);
  }

  @Test
  public void testReadUnknownLength() throws IOException, InterruptedException, TimeoutException, ExecutionException
  {
    testRead(false);
  }

  private void testRead(final boolean knownLength) throws IOException, InterruptedException, TimeoutException, ExecutionException
  {
    final int pipeBufSize = 1024;
    final PipedOutputStream pos = new PipedOutputStream();
    final PipedInputStream pis = new PipedInputStream(pos, pipeBufSize);

    // We use twice the size of the pipe buffer to ensure that the first read
    // will not get all bytes. We can't use more than twice the pipe buffer size
    // because that the reader would effectively fall behind and pos.write(bytes)
    // would block.
    final byte[] bytes = new byte[2 * pipeBufSize];
    for (int i = 0; i < bytes.length; i++)
    {
      bytes[i] = (byte) (i % 256);
    }

    final ExecutorService exec = Executors.newSingleThreadExecutor();
    try
    {
      final Future<ByteString> result = exec.submit(new Callable<ByteString>()
      {
        @Override
        public ByteString call() throws Exception
        {
          return knownLength ? ByteString.read(pis, bytes.length) : ByteString.read(pis);
        }
      });

      pos.write(bytes);
      pos.close();

      Assert.assertEquals(result.get(60, TimeUnit.SECONDS).copyBytes(), bytes);
    }
    finally
    {
      exec.shutdownNow();
    }
  }

  @Test
  public void testAsString() throws UnsupportedEncodingException
  {
    final byte[] bytes = "test string".getBytes(Data.UTF_8_CHARSET);
    final ByteString bs = ByteString.copy(bytes);
    Assert.assertEquals("test string", bs.asString(Data.UTF_8_CHARSET));
    Assert.assertEquals("test", bs.slice(0, 4).asString(Data.UTF_8_CHARSET));
    Assert.assertEquals("string", bs.copySlice(5, 6).asString(Data.UTF_8_CHARSET));

    ByteString twoBs = cons(bs, bs);
    Assert.assertEquals("test stringtest string", twoBs.asString(Data.UTF_8_CHARSET));
  }

  @Test(dataProvider = "byteStrings")
  public void testAsByteBuffer(byte[] bytes, ByteString bs)
  {
    final ByteBuffer buf = ByteBuffer.wrap(bytes);

    Assert.assertEquals(bytes.length, buf.remaining());
    Assert.assertTrue(bs.asByteBuffer().isReadOnly());

    for (byte b : bytes)
    {
      Assert.assertEquals(b, buf.get());
    }
  }

  @Test(dataProvider = "byteStrings")
  public void testAsInputStream(byte[] bytes, ByteString bs) throws IOException
  {
    final InputStream in = bs.asInputStream();
    Assert.assertEquals(bytes.length, in.available());

    final byte[] actual = new byte[bytes.length];
    Assert.assertEquals(bytes.length, in.read(actual));

    Assert.assertEquals(bytes, actual);
  }

  @Test(dataProvider = "byteStrings")
  public void testAsAvroString(byte[] bytes, ByteString bs)
  {
    final String avroString = bs.asAvroString();
    Assert.assertEquals(avroString, Data.bytesToString(bytes));
  }

  @Test
  public void testEquals()
  {
    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4}),
                        ByteString.copy(new byte[] {1,2,3,4}));
    Assert.assertFalse(ByteString.copy(new byte[] {1,2,3,4}).equals(
                       ByteString.copy(new byte[] {5,6,7,8})));

    ByteString bs = ByteString.copy(new byte[] {1,2,3,4});
    Assert.assertNotEquals(bs, bs.slice(0, 3));
    Assert.assertNotEquals(bs, bs.copySlice(0, 3));
    Assert.assertEquals(bs, bs.slice(0, 4));
    Assert.assertEquals(bs, bs.copySlice(0, 4));
    Assert.assertEquals(bs.slice(1, 2), bs.copySlice(1, 2));

    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4,1,2,3,4}), cons(bs, bs));
  }

  @Test
  public void testHashCode()
  {
    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4}).hashCode(),
                        ByteString.copy(new byte[] {1,2,3,4}).hashCode());

    ByteString bs = ByteString.copy(new byte[] {1,2,3,4});
    Assert.assertNotEquals(bs.hashCode(), bs.slice(0, 3).hashCode());
    Assert.assertNotEquals(bs.hashCode(), bs.copySlice(0, 3).hashCode());
    Assert.assertEquals(bs.hashCode(), bs.slice(0, 4).hashCode());
    Assert.assertEquals(bs.hashCode(), bs.copySlice(0, 4).hashCode());
    Assert.assertEquals(bs.slice(1, 2).hashCode(), bs.copySlice(1, 2).hashCode());

    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4,1,2,3,4}).hashCode(), cons(bs, bs).hashCode());

  }

  @Test
  public void testChangeOriginalBytes()
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteString bs = ByteString.copy(bytes);
    final ByteString bsRegion = ByteString.copy(bytes, 1, 2);

    final byte[] bytesCopy = Arrays.copyOf(bytes, bytes.length);
    final byte[] bytesRegionCopy = Arrays.copyOfRange(bytes, 1, 3);
    bytes[0] = 50;
    bytes[2] = 100;

    Assert.assertEquals(bytesCopy, bs.copyBytes());
    Assert.assertFalse(Arrays.equals(bytes, bs.copyBytes()));

    Assert.assertEquals(bytesRegionCopy, bsRegion.copyBytes());
    Assert.assertNotEquals(Arrays.copyOfRange(bytes, 1, 3), bsRegion.copyBytes());
  }

  @Test
  public void testChangeCopiedBytes()
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final byte[] bytesRegion = Arrays.copyOfRange(bytes, 1, 3);
    final ByteString bs = ByteString.copy(bytes);
    final ByteString bsRegion = ByteString.copy(bytes, 1, 2);

    final byte[] bytesCopy = bs.copyBytes();
    final byte[] bytesRegionCopy = bsRegion.copyBytes();
    bytesCopy[0] = 50;
    bytesRegionCopy[0] = 100;

    Assert.assertEquals(bytes, bs.copyBytes());
    Assert.assertFalse(Arrays.equals(bytesCopy, bs.copyBytes()));

    Assert.assertEquals(bytesRegion, bsRegion.copyBytes());
    Assert.assertNotEquals(bytesRegionCopy, bsRegion.copyBytes());
  }

  @Test(dataProvider = "byteStrings")
  public void testWrite(byte[] bytes, ByteString bs) throws IOException
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    bs.write(out);

    Assert.assertEquals(bytes, out.toByteArray());
  }

  @Test
  public void testToStringUpToEight()
  {
    for (int i = 1; i <= 8; i++)
    {
      final byte[] bytes = new byte[i];
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < i; j++)
      {
        bytes[j] = (byte)j;
        sb.append(String.format("%02x", j));
      }

      ByteString bs = ByteString.copy(bytes);
      Assert.assertTrue(bs.toString().contains("bytes=" + sb.toString()));
      if (i == 8)
      {
        Assert.assertTrue(cons(bs.slice(0, 4), bs.slice(4, 4)).toString().contains("bytes=" + sb.toString()));
      }
    }
  }

  @Test
  public void testToStringIncludesHeadAndTail()
  {
    final byte[] bytes = new byte[16384];

    bytes[0] = -34;
    bytes[1] = -83;
    bytes[bytes.length - 2] = -66;
    bytes[bytes.length - 1] = -17;
    String s = ByteString.copy(bytes).toString();
    Assert.assertTrue(s.contains("bytes=dead"));
    Assert.assertTrue(s.contains("beef)"));
  }

  @Test
  public void testToStringNegative()
  {
    // make sure "negative" bytes are represented as positive numbers e.g. ff for -1
    Assert.assertTrue(ByteString.copy(new byte[] { -1 }).toString().contains("bytes=ff"));
  }

  @Test
  public void testToStringBoundedSize()
  {
    final byte[] bytes = new byte[16384];

    // large byte strings should have constant size toString()
    Assert.assertTrue(ByteString.copy(bytes).toString().length() < 100);
  }

  @Test
  public void testSliceToString()
  {
    final byte[] bytes = new byte[1000];
    for (int i = 0; i < 1000; i++)
    {
      bytes[i] = (byte)i;
    }

    ByteString bs = ByteString.copy(bytes);
    ByteString slice = bs.slice(10, 900);
    String sliceToString = slice.toString();
    String expectedToString = ByteString.copy(Arrays.copyOfRange(bytes, 10, 910)).toString();

    Assert.assertEquals(sliceToString, expectedToString);
  }

  @Test(dataProvider = "byteStrings")
  public void testSlice(byte[] bytes, ByteString bs)
  {
    ByteString slice = bs.slice(2, 1);

    Assert.assertEquals(slice.copyBytes(), Arrays.copyOfRange(bytes, 2, 3));

    try
    {
      // try to access the invisible portion of the backing array
      slice.slice(0, 2);
      Assert.fail("Should have failed due to IndexOutOfBound");
    }
    catch (IndexOutOfBoundsException ex)
    {
      // expected
    }
  }

  @Test
  public void complexSlice()
  {
    byte[] bytes = new byte[]{1,2,3};
    ByteString bs = ByteString.copy(bytes);
    ByteString twoBs = cons(bs, bs);
    ByteString threeBs = cons(bs, twoBs);

    byte[] threeBytes = new byte[] {1,2,3,1,2,3,1,2,3};

    ByteString slice = threeBs.slice(1, 7);
    ByteString expected = ByteString.copy(Arrays.copyOfRange(threeBytes, 1, 8));
    Assert.assertEquals(slice, expected);
    Assert.assertEquals(slice.hashCode(), expected.hashCode());

    ByteString slice2 = slice.slice(1, 5);
    ByteString expected2 = ByteString.copy(Arrays.copyOfRange(threeBytes, 2, 7));
    Assert.assertEquals(slice2, expected2);
    Assert.assertEquals(slice2.hashCode(), expected2.hashCode());

    try
    {
      // try to access the invisible portion of the backing array
      slice2.slice(0, 6);
      Assert.fail("Should have failed due to IndexOutOfBound");
    }
    catch (IndexOutOfBoundsException ex)
    {
      // expected
    }

    Assert.assertEquals(twoBs.slice(2, 2), slice2.slice(0, 2));
  }

  @Test(dataProvider = "byteStrings")
  public void testCopySlice(byte[] bytes, ByteString bs)
  {
    ByteString substr = bs.copySlice(2, 1);

    Assert.assertEquals(substr.copyBytes(), Arrays.copyOfRange(bytes, 2, 3));
  }

  @Test
  public void testBuilder()
  {
    ByteString.Builder builder = new ByteString.Builder();

    Assert.assertEquals(builder.build(), ByteString.empty());

    final byte[] bytes = new byte[1000];
    for (int i = 0; i < 1000; i++)
    {
      bytes[i] = (byte)i;
    }

    ByteString bs = ByteString.copy(bytes);

    builder.append(bs);

    Assert.assertSame(builder.build(), bs);

    builder.append(bs);
    builder.append(bs);
    ByteString newBs = builder.build();

    final byte[] expectedBytes = new byte[3000];
    System.arraycopy(bytes, 0, expectedBytes, 0, 1000);
    System.arraycopy(bytes, 0, expectedBytes, 1000, 1000);
    System.arraycopy(bytes, 0, expectedBytes, 2000, 1000);

    Assert.assertEquals(newBs.copyBytes(), expectedBytes);

    builder.append(newBs);
    builder.append(bs);
    ByteString newerBs = builder.build();

    final byte[] expectedNewerBytes = new byte[7000];
    System.arraycopy(expectedBytes, 0, expectedNewerBytes, 0, 3000);
    System.arraycopy(expectedBytes, 0, expectedNewerBytes, 3000, 3000);
    System.arraycopy(bytes, 0, expectedNewerBytes, 6000, 1000);

    Assert.assertEquals(newerBs.copyBytes(), expectedNewerBytes);
  }

  @Test
  public void testNullCheckOnBuilder()
  {
    ByteString.Builder builder = new ByteString.Builder();
    try
    {
      builder.append(null);
      Assert.fail();
    } catch (NullPointerException ex) {
      Assert.assertEquals(ex.getMessage(), "dataChunk is null");
    }
  }

  @DataProvider
  public Object[][] byteStrings()
  {
    final byte[] bytes = new byte[] {1,2,3,4,5};
    final ByteString bs = ByteString.copy(bytes);
    final ByteString twoBs = cons(bs, bs);
    final ByteString threeBs = cons(bs, twoBs);
    final byte[] twoBsBytes = new byte[] {1,2,3,4,5,1,2,3,4,5};
    final byte[] threeBsBytes = new byte[] {1,2,3,4,5,1,2,3,4,5,1,2,3,4,5};

    return new Object[][]{
        {bytes, bs},
        {Arrays.copyOfRange(bytes, 1, 4), ByteString.copy(bytes, 1, 3)},
        {Arrays.copyOfRange(bytes, 1, 5), bs.slice(1, 4)},  // slice of simple byte string
        {Arrays.copyOfRange(bytes, 1, 4), bs.copySlice(1, 3)}, // copy slice of simple byte string
        {twoBsBytes, twoBs}, // composite byte string with two backing byte arrays built with two simple byte strings
        {threeBsBytes, threeBs}, // composite byte string with three backing byte arrays built with a simple byte string and a composite byte string
        {Arrays.copyOfRange(threeBsBytes, 1, 12), threeBs.slice(1, 11)} // slice of composite byte string
    };
  }

  ByteString cons(ByteString bs1, ByteString bs2)
  {
    ByteString.Builder builder = new ByteString.Builder();
    return builder.append(bs1).append(bs2).build();
  }
}
