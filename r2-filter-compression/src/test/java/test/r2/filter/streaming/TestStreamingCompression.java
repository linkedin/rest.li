/*
   Copyright (c) 2015 LinkedIn Corp.

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

package test.r2.filter.streaming;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.compression.CompressionException;
import com.linkedin.r2.filter.compression.streaming.Bzip2Compressor;
import com.linkedin.r2.filter.compression.streaming.DeflateCompressor;
import com.linkedin.r2.filter.compression.streaming.GzipCompressor;
import com.linkedin.r2.filter.compression.streaming.SnappyCompressor;
import com.linkedin.r2.filter.compression.streaming.StreamingCompressor;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.iq80.snappy.SnappyOutputStream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Ang Xu
 */
public class TestStreamingCompression
{
  private static final int BUF_SIZE = 4 * 1024 * 1024;

  private ExecutorService _executor;

  @BeforeClass
  public void setup()
  {
    _executor = Executors.newCachedThreadPool();
  }

  @AfterClass
  public void teardown()
  {
    _executor.shutdown();
  }

  @Test
  public void testSnappyCompressor()
      throws IOException, InterruptedException, CompressionException, ExecutionException
  {
    StreamingCompressor compressor = new SnappyCompressor(_executor);
    final byte[] origin = new byte[BUF_SIZE];
    Arrays.fill(origin, (byte)'a');

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SnappyOutputStream snappy = new SnappyOutputStream(out);
    IOUtils.write(origin, snappy);
    snappy.close();
    byte[] compressed = out.toByteArray();

    testCompress(compressor, origin, compressed);
    testDecompress(compressor, origin, compressed);
    testCompressThenDecompress(compressor, origin);
  }

  @Test
  public void testGzipCompressor()
      throws IOException, InterruptedException, CompressionException, ExecutionException
  {
    StreamingCompressor compressor = new GzipCompressor(_executor);
    final byte[] origin = new byte[BUF_SIZE];
    Arrays.fill(origin, (byte) 'b');

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(out);
    IOUtils.write(origin, gzip);
    gzip.close();
    byte[] compressed = out.toByteArray();

    testCompress(compressor, origin, compressed);
    testDecompress(compressor, origin, compressed);
    testCompressThenDecompress(compressor, origin);
  }

  @Test
  public void testBzip2Compressor()
      throws IOException, InterruptedException, CompressionException, ExecutionException
  {
    StreamingCompressor compressor = new Bzip2Compressor(_executor);
    final byte[] origin = new byte[BUF_SIZE];
    Arrays.fill(origin, (byte)'c');

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BZip2CompressorOutputStream bzip = new BZip2CompressorOutputStream(out);
    IOUtils.write(origin, bzip);
    bzip.close();
    byte[] compressed = out.toByteArray();

    testCompress(compressor, origin, compressed);
    testDecompress(compressor, origin, compressed);
    testCompressThenDecompress(compressor, origin);
  }

  @Test
  public void testDeflateCompressor()
      throws IOException, InterruptedException, CompressionException, ExecutionException
  {
    StreamingCompressor compressor = new DeflateCompressor(_executor);
    final byte[] origin = new byte[BUF_SIZE];
    Arrays.fill(origin, (byte)'c');

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DeflaterOutputStream zlib = new DeflaterOutputStream(out);
    IOUtils.write(origin, zlib);
    zlib.close();
    byte[] compressed = out.toByteArray();

    testCompress(compressor, origin, compressed);
    testDecompress(compressor, origin, compressed);
    testCompressThenDecompress(compressor, origin);
  }

  private void testCompress(StreamingCompressor compressor, byte[] uncompressed, byte[] compressed)
      throws CompressionException, ExecutionException, InterruptedException
  {
    ByteWriter writer = new ByteWriter(uncompressed);
    EntityStream uncompressedStream = EntityStreams.newEntityStream(writer);
    EntityStream compressedStream = compressor.deflate(uncompressedStream);

    FutureCallback<byte[]> callback = new FutureCallback<byte[]>();
    compressedStream.setReader(new ByteReader(callback));

    byte[] result = callback.get();
    Assert.assertEquals(result, compressed);
  }

  private void testDecompress(StreamingCompressor compressor, byte[] uncompressed, byte[] compressed)
      throws CompressionException, ExecutionException, InterruptedException
  {
    ByteWriter writer = new ByteWriter(compressed);
    EntityStream compressedStream = EntityStreams.newEntityStream(writer);
    EntityStream uncompressedStream = compressor.inflate(compressedStream);

    FutureCallback<byte[]> callback = new FutureCallback<byte[]>();
    uncompressedStream.setReader(new ByteReader(callback));

    byte[] result = callback.get();
    Assert.assertEquals(result, uncompressed);
  }

  private void testCompressThenDecompress(StreamingCompressor compressor, byte[] origin)
      throws CompressionException, ExecutionException, InterruptedException
  {
    ByteWriter writer = new ByteWriter(origin);
    EntityStream uncompressedStream = EntityStreams.newEntityStream(writer);
    EntityStream compressedStream = compressor.deflate(uncompressedStream);

    EntityStream decompressedStream = compressor.inflate(compressedStream);

    FutureCallback<byte[]> callback = new FutureCallback<byte[]>();
    decompressedStream.setReader(new ByteReader(callback));

    byte[] result = callback.get();
    Assert.assertEquals(result, origin);
  }

  private static class ByteReader implements Reader
  {
    private final Callback<byte[]> _callback;
    private ReadHandle _rh;
    private byte[] _bytes;

    public ByteReader(Callback<byte[]> callback)
    {
      _callback = callback;
    }

    @Override
    public void onInit(ReadHandle rh)
    {
      _rh = rh;
      _rh.request(1);
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      if (_bytes == null)
      {
        _bytes = data.copyBytes();
      }
      else
      {
        byte[] bytes = new byte[_bytes.length + data.length()];
        System.arraycopy(_bytes, 0, bytes, 0, _bytes.length);
        System.arraycopy(data.copyBytes(), 0, bytes, _bytes.length, data.length());
        _bytes = bytes;
      }
      _rh.request(1);
    }

    @Override
    public void onDone()
    {
      _callback.onSuccess(_bytes);
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onError(e);
    }
  }

  private static class ByteWriter extends ByteStringWriter
  {
    public ByteWriter(byte[] bytes)
    {
      super(ByteString.copy(bytes));
    }
  }
}
