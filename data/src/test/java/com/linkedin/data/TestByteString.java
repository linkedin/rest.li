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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

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
  }

  @Test
  public void testCopyString()
  {
    final String str = "test string";
    final ByteString bs = ByteString.copyString(str, "UTF-8");
    Assert.assertEquals("test string", bs.asString("UTF-8"));
  }

  @Test
  public void testAsString() throws UnsupportedEncodingException
  {
    final byte[] bytes = "test string".getBytes(Data.UTF_8_CHARSET);
    final ByteString bs = ByteString.copy(bytes);
    Assert.assertEquals("test string", bs.asString(Data.UTF_8_CHARSET));
  }

  @Test
  public void testAsByteBuffer()
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteBuffer buf = ByteBuffer.wrap(bytes);
    final ByteString bs = ByteString.copy(bytes);

    Assert.assertEquals(4, buf.remaining());
    Assert.assertTrue(bs.asByteBuffer().isReadOnly());

    for (byte b : bytes)
    {
      Assert.assertEquals(b, buf.get());
    }
  }

  @Test
  public void testAsInputStream() throws IOException
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteString bs = ByteString.copy(bytes);

    final InputStream in = bs.asInputStream();
    Assert.assertEquals(bytes.length, in.available());

    final byte[] actual = new byte[bytes.length];
    Assert.assertEquals(bytes.length, in.read(actual));

    Assert.assertEquals(bytes, actual);
  }

  @Test
  public void testEquals()
  {
    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4}),
                        ByteString.copy(new byte[] {1,2,3,4}));
    Assert.assertFalse(ByteString.copy(new byte[] {1,2,3,4}).equals(
                       ByteString.copy(new byte[] {5,6,7,8})));
  }

  @Test
  public void testHashCode()
  {
    Assert.assertEquals(ByteString.copy(new byte[] {1,2,3,4}).hashCode(),
                        ByteString.copy(new byte[] {1,2,3,4}).hashCode());
  }

  @Test
  public void testChangeOriginalBytes()
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteString bs = ByteString.copy(bytes);

    final byte[] bytesCopy = Arrays.copyOf(bytes, bytes.length);
    bytes[0] = 50;

    Assert.assertEquals(bytesCopy, bs.copyBytes());
    Assert.assertFalse(Arrays.equals(bytes, bs.copyBytes()));
  }

  @Test
  public void testChangeCopiedBytes()
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteString bs = ByteString.copy(bytes);

    final byte[] bytesCopy = bs.copyBytes();
    bytesCopy[0] = 50;

    Assert.assertEquals(bytes, bs.copyBytes());
    Assert.assertFalse(Arrays.equals(bytesCopy, bs.copyBytes()));
  }

  @Test
  public void testWrite() throws IOException
  {
    final byte[] bytes = new byte[] {1,2,3,4};
    final ByteString bs = ByteString.copy(bytes);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    bs.write(out);

    Assert.assertEquals(bytes, out.toByteArray());
  }
}
