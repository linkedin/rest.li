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

package com.linkedin.multipart;


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import com.linkedin.multipart.utils.MIMEDataPart;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;

import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.*;


/**
 * Using Javax.mail on the server side to verify the integrity of our RFC implementation of the
 * {@link com.linkedin.multipart.MultiPartMIMEWriter}. There is no way to use mockito to mock EntityStreams.newEntityStream()
 * Therefore we have no choice but to use R2's functionality here.
 *
 * @author Karim Vidhani
 */
public class TestMIMEWriter extends AbstractMIMEUnitTest
{
  byte[] _normalBodyData;
  Map<String, String> _normalBodyHeaders;

  byte[] _headerLessBodyData;

  Map<String, String> _bodyLessHeaders;

  MIMEDataPart _normalBody;
  MIMEDataPart _headerLessBody;
  MIMEDataPart _bodyLessBody;
  MIMEDataPart _purelyEmptyBody;

  @BeforeTest
  public void setup()
  {
    _normalBodyData = "abc".getBytes();
    _normalBodyHeaders = new HashMap<String, String>();
    _normalBodyHeaders.put("simpleheader", "simplevalue");

    //Second body has no headers
    _headerLessBodyData = "def".getBytes();

    //Third body has only headers
    _bodyLessHeaders = new HashMap<String, String>();
    _normalBodyHeaders.put("header1", "value1");
    _normalBodyHeaders.put("header2", "value2");
    _normalBodyHeaders.put("header3", "value3");

    _normalBody = new MIMEDataPart(ByteString.copy(_normalBodyData), _normalBodyHeaders);

    _headerLessBody = new MIMEDataPart(ByteString.copy(_headerLessBodyData), Collections.<String, String>emptyMap());

    _bodyLessBody = new MIMEDataPart(ByteString.empty(), _bodyLessHeaders);

    _purelyEmptyBody = new MIMEDataPart(ByteString.empty(), Collections.<String, String>emptyMap());
  }

  @DataProvider(name = "singleDataSources")
  public Object[][] singleDataSources() throws Exception
  {
    return new Object[][]
        {
            {ByteString.copy(_normalBodyData), _normalBodyHeaders},
            {ByteString.copy(_headerLessBodyData), Collections.<String, String>emptyMap()},
            {ByteString.empty(), _bodyLessHeaders},
            {ByteString.empty(), Collections.<String, String>emptyMap()}
        };
  }

  @Test(dataProvider = "singleDataSources")
  public void testSingleDataSource(final ByteString body, final Map<String, String> headers) throws Exception
  {
    final MIMEDataPart expectedMultiPartMIMEDataPart = new MIMEDataPart(body, headers);

    final MultiPartMIMEInputStream singleDataSource =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(body.copyBytes()), _scheduledExecutorService,
                                             headers).build();

    final MultiPartMIMEWriter multiPartMIMEWriter =
        new MultiPartMIMEWriter.Builder("preamble", "epilogue").appendDataSource(singleDataSource).build();

    final FutureCallback<ByteString> futureCallback = new FutureCallback<ByteString>();
    final FullEntityReader fullEntityReader = new FullEntityReader(futureCallback);
    multiPartMIMEWriter.getEntityStream().setReader(fullEntityReader);
    futureCallback.get(_testTimeout, TimeUnit.MILLISECONDS);

    final StreamRequest multiPartMIMEStreamRequest =
        MultiPartMIMEStreamRequestFactory
            .generateMultiPartMIMEStreamRequest(URI.create("localhost"), "mixed", multiPartMIMEWriter,
                                                Collections.<String, String>emptyMap());

    JavaxMailMultiPartMIMEReader javaxMailMultiPartMIMEReader =
        new JavaxMailMultiPartMIMEReader(multiPartMIMEStreamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER),
                                         futureCallback.get());
    javaxMailMultiPartMIMEReader.parseRequestIntoParts();

    List<MIMEDataPart> dataSourceList = javaxMailMultiPartMIMEReader._dataSourceList;

    Assert.assertEquals(dataSourceList.size(), 1);
    Assert.assertEquals(dataSourceList.get(0), expectedMultiPartMIMEDataPart);
    //Javax mail incorrectly adds the CRLF for the first boundary to the end of the preamble, so we trim
    Assert.assertEquals(javaxMailMultiPartMIMEReader._preamble.trim(), "preamble");
  }

  @Test
  public void testMultipleDataSources() throws Exception
  {
    final List<MIMEDataPart> expectedParts = new ArrayList<MIMEDataPart>();
    expectedParts.add(_normalBody);
    expectedParts.add(_normalBody);
    expectedParts.add(_headerLessBody);
    expectedParts.add(_normalBody);
    expectedParts.add(_bodyLessBody);
    expectedParts.add(_purelyEmptyBody);
    expectedParts.add(_purelyEmptyBody);
    expectedParts.add(_headerLessBody);
    expectedParts.add(_headerLessBody);
    expectedParts.add(_headerLessBody);
    expectedParts.add(_normalBody);
    expectedParts.add(_bodyLessBody);

    final List<MultiPartMIMEDataSourceWriter> inputStreamDataSources = new ArrayList<MultiPartMIMEDataSourceWriter>();
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData),
                                                                    _scheduledExecutorService,
                                                                    _normalBodyHeaders).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData),
                                                                    _scheduledExecutorService,
                                                                    _normalBodyHeaders).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData),
                                                                    _scheduledExecutorService,
                                                                    _normalBodyHeaders).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]),
                                                                    _scheduledExecutorService,
                                                                    _bodyLessHeaders).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData),
                                                                    _scheduledExecutorService,
                                                                    Collections.<String, String>emptyMap()).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData),
                                                                    _scheduledExecutorService,
                                                                    _normalBodyHeaders).build());
    inputStreamDataSources.add(new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]),
                                                                    _scheduledExecutorService,
                                                                    _bodyLessHeaders).build());

    final MultiPartMIMEWriter.Builder multiPartMIMEWriterBuilder =
        new MultiPartMIMEWriter.Builder("preamble", "epilogue").appendDataSources(inputStreamDataSources);

    Assert.assertEquals(multiPartMIMEWriterBuilder.getCurrentSize(), inputStreamDataSources.size());

    final MultiPartMIMEWriter multiPartMIMEWriter = multiPartMIMEWriterBuilder.build();

    final FutureCallback<ByteString> futureCallback = new FutureCallback<ByteString>();
    final FullEntityReader fullEntityReader = new FullEntityReader(futureCallback);
    multiPartMIMEWriter.getEntityStream().setReader(fullEntityReader);
    futureCallback.get(_testTimeout, TimeUnit.MILLISECONDS);

    final StreamRequest multiPartMIMEStreamRequest =
        MultiPartMIMEStreamRequestFactory
            .generateMultiPartMIMEStreamRequest(URI.create("localhost"), "mixed", multiPartMIMEWriter,
                                                Collections.<String, String>emptyMap());

    JavaxMailMultiPartMIMEReader javaxMailMultiPartMIMEReader =
        new JavaxMailMultiPartMIMEReader(multiPartMIMEStreamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER),
                                         futureCallback.get());
    javaxMailMultiPartMIMEReader.parseRequestIntoParts();

    List<MIMEDataPart> dataSourceList = javaxMailMultiPartMIMEReader._dataSourceList;

    Assert.assertEquals(dataSourceList.size(), 12);
    for (int i = 0; i < dataSourceList.size(); i++)
    {
      Assert.assertEquals(dataSourceList.get(i), expectedParts.get(i));
    }

    //Javax mail incorrectly adds the CRLF for the first boundary to the end of the preamble, so we trim
    Assert.assertEquals(javaxMailMultiPartMIMEReader._preamble.trim(), "preamble");
  }

  @DataProvider(name = "prependDataSources")
  public Object[][] prependDataSources() throws Exception
  {
    final List<MIMEDataPart> expectedParts = new ArrayList<MIMEDataPart>();
    expectedParts.add(_normalBody);
    expectedParts.add(_headerLessBody);
    expectedParts.add(_purelyEmptyBody);

    //We will perform two requests here. One without a preamble and one with a preamble to ensure that we prepend
    //in the correct location. Note we need to create multiple writers because each of these requests will drain the
    //writers.

    //With preamble:
    final MultiPartMIMEDataSourceWriter normalWriterPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData), _scheduledExecutorService,
                                             _normalBodyHeaders).build();

    final MultiPartMIMEDataSourceWriter headerLessBodyWriterPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData), _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).build();

    final MultiPartMIMEDataSourceWriter purelyEmptyBodyWriterPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]), _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).build();

    final List<MultiPartMIMEDataSourceWriter> withPreambleDataSourceWriterList =
        Collections.unmodifiableList(Arrays.asList(purelyEmptyBodyWriterPreamble, headerLessBodyWriterPreamble,
            normalWriterPreamble));

    final MultiPartMIMEWriter.Builder multiPartMIMEWriterWithPreamble = new MultiPartMIMEWriter.Builder("preamble", "epilogue");

    //Without preamble:
    final MultiPartMIMEDataSourceWriter normalWriterNoPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBodyData), _scheduledExecutorService,
                                             _normalBodyHeaders).build();

    final MultiPartMIMEDataSourceWriter headerLessBodyWriterNoPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBodyData), _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).build();

    final MultiPartMIMEDataSourceWriter purelyEmptyBodyWriterNoPreamble =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(new byte[0]), _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).build();

    final List<MultiPartMIMEDataSourceWriter> withoutPreambleDataSourceWriterList =
        Collections.unmodifiableList(Arrays.asList(purelyEmptyBodyWriterNoPreamble, headerLessBodyWriterNoPreamble,
            normalWriterNoPreamble));

    final MultiPartMIMEWriter.Builder multiPartMIMEWriterWithoutPreamble = new MultiPartMIMEWriter.Builder("", "epilogue");

    return new Object[][]
        {
            {multiPartMIMEWriterWithPreamble, withPreambleDataSourceWriterList, expectedParts, "preamble", 3},
            {multiPartMIMEWriterWithoutPreamble, withoutPreambleDataSourceWriterList, expectedParts, null, 3}
        };
  }

  @Test(dataProvider = "prependDataSources")
  public void testPrependDataSources(final MultiPartMIMEWriter.Builder builder,
                                     final List<MultiPartMIMEDataSourceWriter> prependDataSources,
                                     final List<MIMEDataPart> expectedParts,
                                     final String expectedPreamble, final int expectedSize) throws Exception
  {
    for (final MultiPartMIMEDataSourceWriter dataSourceWriter : prependDataSources)
    {
      builder.prependDataSource(dataSourceWriter);
    }

    Assert.assertEquals(builder.getCurrentSize(), expectedSize);

    final MultiPartMIMEWriter writer = builder.build();
    final FutureCallback<ByteString> futureCallback = new FutureCallback<ByteString>();
    final FullEntityReader fullEntityReader = new FullEntityReader(futureCallback);
    writer.getEntityStream().setReader(fullEntityReader);
    futureCallback.get(_testTimeout, TimeUnit.MILLISECONDS);

    final StreamRequest multiPartMIMEStreamRequest =
        MultiPartMIMEStreamRequestFactory
            .generateMultiPartMIMEStreamRequest(URI.create("localhost"), "mixed", writer,
                                                Collections.<String, String>emptyMap());

    final JavaxMailMultiPartMIMEReader javaxMailMultiPartMIMEReader =
        new JavaxMailMultiPartMIMEReader(multiPartMIMEStreamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER),
                                         futureCallback.get());
    javaxMailMultiPartMIMEReader.parseRequestIntoParts();

    List<MIMEDataPart> dataSourceList = javaxMailMultiPartMIMEReader._dataSourceList;

    Assert.assertEquals(dataSourceList.size(), 3);
    for (int i = 0; i < dataSourceList.size(); i++)
    {
      Assert.assertEquals(dataSourceList.get(i), expectedParts.get(i));
    }

    //Javax mail incorrectly adds the CRLF for the first boundary to the end of the preamble, so we trim
    Assert.assertEquals(javaxMailMultiPartMIMEReader._preamble != null ? javaxMailMultiPartMIMEReader._preamble.trim() : null,
                        expectedPreamble);
  }

  private static class JavaxMailMultiPartMIMEReader
  {
    final String _contentTypeHeaderValue;
    final ByteString _payload;
    String _preamble; //javax mail only supports reading the preamble
    final List<MIMEDataPart> _dataSourceList = new ArrayList<MIMEDataPart>();

    private JavaxMailMultiPartMIMEReader(final String contentTypeHeaderValue, final ByteString payload)
    {
      _contentTypeHeaderValue = contentTypeHeaderValue;
      _payload = payload;
    }

    @SuppressWarnings("rawtypes")
    private void parseRequestIntoParts()
    {
      final DataSource dataSource = new DataSource()
      {
        @Override
        public InputStream getInputStream() throws IOException
        {
          return new ByteArrayInputStream(_payload.copyBytes());
        }

        @Override
        public OutputStream getOutputStream() throws IOException
        {
          return null;
        }

        @Override
        public String getContentType()
        {
          return _contentTypeHeaderValue;
        }

        @Override
        public String getName()
        {
          return null;
        }
      };

      try
      {
        final MimeMultipart mimeBody = new MimeMultipart(dataSource);
        for (int i = 0; i < mimeBody.getCount(); i++)
        {
          final BodyPart bodyPart = mimeBody.getBodyPart(i);
          try
          {
            //For our purposes, javax mail converts the body part's content (based on headers) into a string
            final ByteString partData = ByteString.copyString((String) bodyPart.getContent(), Charset.defaultCharset());

            final Map<String, String> partHeaders = new HashMap<String, String>();
            final Enumeration allHeaders = bodyPart.getAllHeaders();
            while (allHeaders.hasMoreElements())
            {
              final Header header = (Header) allHeaders.nextElement();
              partHeaders.put(header.getName(), header.getValue());
            }
            final MIMEDataPart tempDataSource = new MIMEDataPart(partData, partHeaders);
            _dataSourceList.add(tempDataSource);
          }
          catch (Exception exception)
          {
            Assert.fail("Failed to read body content due to " + exception);
          }
        }
        _preamble = mimeBody.getPreamble();
      }
      catch (MessagingException messagingException)
      {
        Assert.fail("Failed to read in request multipart mime body");
      }
    }
  }

  @Test
  public void testMimeWriterWithLargePayload() throws InterruptedException, ExecutionException {
    long size = 0l;

    List<MultiPartMIMEDataSourceWriter> list = new ArrayList<>();
    for (int i = 0 ; i < 20; i++) {
      final MultiPartMIMEInputStream bodyADataSource =
          new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_6.getPartData().copyBytes()),
              _scheduledExecutorService, BODY_6.getPartHeaders()).build();
      list.add(bodyADataSource);
    }
    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(list).build();

    EntityStreamReader reader = new EntityStreamReader();
    writer.getEntityStream().setReader(reader);
    CompletableFuture<Optional<ByteString>> future;
    do {
      future = reader.readNextChunk();
      if (future.get().isPresent()) {
        size += future.get().get().length();
      }
    } while (future.get().isPresent());
    Assert.assertTrue(size > 20L * BODY_6_SIZE);
  }

  @Test
  public void testMimeWriterWithLargeNumberOfStreams() throws InterruptedException, ExecutionException {
    long size = 0l;

    List<MultiPartMIMEDataSourceWriter> list = new ArrayList<>();
    for (int i = 0 ; i < 2000; i++) {
      final MultiPartMIMEInputStream bodyADataSource =
          new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_7.getPartData().copyBytes()),
              _scheduledExecutorService, BODY_7.getPartHeaders()).build();
      list.add(bodyADataSource);
    }
    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(list).build();

    EntityStreamReader reader = new EntityStreamReader();
    writer.getEntityStream().setReader(reader);
    CompletableFuture<Optional<ByteString>> future;
    do {
      future = reader.readNextChunk();
      if (future.get().isPresent()) {
        size += future.get().get().length();
      }
    } while (future.get().isPresent());
    Assert.assertTrue(size > 2000L * BODY_7_SIZE);
  }

  // mimic the reader behavior in Play
  private static class EntityStreamReader implements Reader {
    private final AtomicBoolean _done = new AtomicBoolean(false);
    private ReadHandle _rh;
    private ConcurrentLinkedQueue<CompletableFuture<Optional<ByteString>>> _completableFutures =
        new ConcurrentLinkedQueue<>();

    @Override
    public void onInit(ReadHandle rh) {
      _rh = rh;
    }

    @Override
    public void onDataAvailable(com.linkedin.data.ByteString data) {
      if (!data.isEmpty()) {
        _completableFutures.remove()
            .complete(Optional.of(data));
      } else {
        _rh.request(1);
      }
    }

    @Override
    public synchronized void onDone() {
      _done.set(true);
      // When stream is done, notify all remaining promises
      _completableFutures.forEach(completableFuture -> {
        if (!completableFuture.isDone()) {
          completableFuture.complete(Optional.empty());
        }
      });
      _completableFutures.clear();
    }

    @Override
    public synchronized void onError(Throwable e) {
      // When stream is wrong, notify all remaining promises
      _completableFutures.forEach(completableFuture -> completableFuture.completeExceptionally(e));
      _completableFutures.clear();
    }

    public CompletableFuture<Optional<ByteString>> readNextChunk() {
      CompletableFuture<Optional<ByteString>> completableFuture = new CompletableFuture<>();
      _completableFutures.add(completableFuture);
      if (_done.get()) {
        completableFuture.complete(Optional.empty());
      } else {
        _rh.request(1);

      }
      return completableFuture;
    }
  }
}