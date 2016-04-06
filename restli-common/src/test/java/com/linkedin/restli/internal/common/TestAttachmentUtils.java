/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.common;


import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEStreamRequestFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.multipart.utils.MIMETestUtils;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSourceIterator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link AttachmentUtils} which are used for rest.li attachment streaming. These tests verify that the rest.li
 * layer uses the MultiPartMIMEWriter layer correctly in the face of different types of data sources.
 *
 * @author Karim Vidhani
 */
public class TestAttachmentUtils
{
  @Test
  public void testSingleAttachment()
  {
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final List<RestLiTestAttachmentDataSource> testAttachmentDataSources = generateTestDataSources();

    for (RestLiTestAttachmentDataSource dataSource : testAttachmentDataSources)
    {
      AttachmentUtils.appendSingleAttachmentToBuilder(builder, dataSource);
    }

    final StreamRequest streamRequest = MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(
        URI.create("foo"), "related", builder.build());

    final MultiPartMIMEReader streamRequestReader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final CountDownLatch streamRequestReaderLatch = new CountDownLatch(1);
    final MIMETestUtils.MultiPartMIMEFullReaderCallback
        streamRequestReaderCallback = new MIMETestUtils.MultiPartMIMEFullReaderCallback(streamRequestReaderLatch);
    streamRequestReader.registerReaderCallback(streamRequestReaderCallback);
    try
    {
      streamRequestReaderLatch.await(3000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException interruptedException)
    {
      Assert.fail();
    }
    verifyAttachments(streamRequestReaderCallback.getSinglePartMIMEReaderCallbacks(), testAttachmentDataSources);
  }

  @Test
  public void testMultipleAttachments()
  {
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final List<RestLiTestAttachmentDataSource> testAttachmentDataSources = generateTestDataSources();

    final RestLiTestAttachmentDataSourceIterator dataSourceIterator = new RestLiTestAttachmentDataSourceIterator(testAttachmentDataSources,
                                                                                                                 new IllegalArgumentException());

    //Let each data source know its parent, so that when the data source is done, it can notify it's parent to call onNewDataSourceWriter()
    for (final RestLiTestAttachmentDataSource dataSource : testAttachmentDataSources)
    {
      dataSource.setParentDataSourceIterator(dataSourceIterator);
    }

    AttachmentUtils.appendMultipleAttachmentsToBuilder(builder, dataSourceIterator);

    final StreamRequest streamRequest = MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(
        URI.create("foo"), "related", builder.build());

    final MultiPartMIMEReader streamRequestReader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final CountDownLatch streamRequestReaderLatch = new CountDownLatch(1);
    final MIMETestUtils.MultiPartMIMEFullReaderCallback
        streamRequestReaderCallback = new MIMETestUtils.MultiPartMIMEFullReaderCallback(streamRequestReaderLatch);
    streamRequestReader.registerReaderCallback(streamRequestReaderCallback);
    try
    {
      streamRequestReaderLatch.await(3000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException interruptedException)
    {
      Assert.fail();
    }
    verifyAttachments(streamRequestReaderCallback.getSinglePartMIMEReaderCallbacks(), testAttachmentDataSources);
  }

  @Test
  public void testMixtureOfAttachments()
  {
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final List<RestLiTestAttachmentDataSource> iteratorDataSources = generateTestDataSources();

    final RestLiTestAttachmentDataSourceIterator dataSourceIterator = new RestLiTestAttachmentDataSourceIterator(iteratorDataSources,
                                                                                                                 new IllegalArgumentException());

    //Let each data source know its parent, so that when the data source is done, it can notify it's parent to call onNewDataSourceWriter()
    for (final RestLiTestAttachmentDataSource dataSource : iteratorDataSources)
    {
      dataSource.setParentDataSourceIterator(dataSourceIterator);
    }

    //Now one at the beginning
    final RestLiTestAttachmentDataSource dataSourceBeginning = RestLiTestAttachmentDataSource.createWithRandomPayload("BEG");

    //And one at the end
    final RestLiTestAttachmentDataSource dataSourceEnd = RestLiTestAttachmentDataSource.createWithRandomPayload("END");

    AttachmentUtils.appendSingleAttachmentToBuilder(builder, dataSourceBeginning);
    AttachmentUtils.appendMultipleAttachmentsToBuilder(builder, dataSourceIterator);
    AttachmentUtils.appendSingleAttachmentToBuilder(builder, dataSourceEnd);

    final StreamRequest streamRequest = MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(
        URI.create("foo"), "related", builder.build());

    final MultiPartMIMEReader streamRequestReader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final CountDownLatch streamRequestReaderLatch = new CountDownLatch(1);
    final MIMETestUtils.MultiPartMIMEFullReaderCallback
        streamRequestReaderCallback = new MIMETestUtils.MultiPartMIMEFullReaderCallback(streamRequestReaderLatch);
    streamRequestReader.registerReaderCallback(streamRequestReaderCallback);
    try
    {
      streamRequestReaderLatch.await(3000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException interruptedException)
    {
      Assert.fail();
    }

    final List<RestLiTestAttachmentDataSource> allAttachmentDataSources = new ArrayList<>();
    allAttachmentDataSources.add(dataSourceBeginning);
    allAttachmentDataSources.addAll(iteratorDataSources);
    allAttachmentDataSources.add(dataSourceEnd);
    verifyAttachments(streamRequestReaderCallback.getSinglePartMIMEReaderCallbacks(), allAttachmentDataSources);
  }

  @Test
  public void testMixtureOfAttachmentsAbort()
  {
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final List<RestLiTestAttachmentDataSource> iteratorDataSources = generateTestDataSources();

    final RestLiTestAttachmentDataSourceIterator dataSourceIterator = new RestLiTestAttachmentDataSourceIterator(iteratorDataSources,
                                                                                                                 new IllegalArgumentException());

    //Let each data source know its parent, so that when the data source is done, it can notify it's parent to call onNewDataSourceWriter()
    for (final RestLiTestAttachmentDataSource dataSource : iteratorDataSources)
    {
      dataSource.setParentDataSourceIterator(dataSourceIterator);
    }

    //Now one at the beginning
    final RestLiTestAttachmentDataSource dataSourceBeginning = RestLiTestAttachmentDataSource.createWithRandomPayload("BEG");

    //And one at the end
    final RestLiTestAttachmentDataSource dataSourceEnd = RestLiTestAttachmentDataSource.createWithRandomPayload("END");

    AttachmentUtils.appendSingleAttachmentToBuilder(builder, dataSourceBeginning);
    AttachmentUtils.appendMultipleAttachmentsToBuilder(builder, dataSourceIterator);
    AttachmentUtils.appendSingleAttachmentToBuilder(builder, dataSourceEnd);

    //Abort everything. We want to make sure that our process of wrapping the rest.li data sources into the multipart
    //mime layer correctly propagates the abandon/aborts.
    builder.build().abortAllDataSources(new IllegalStateException());

    Assert.assertTrue(dataSourceIterator.dataSourceIteratorAbandoned());
    Assert.assertTrue(dataSourceBeginning.dataSourceAborted());
    Assert.assertTrue(dataSourceEnd.dataSourceAborted());

    //This last part is technically not necessary but it is good to provide an example of how a data source iterator
    //would handle an abort. In this specific case, the data source iterator informs any potential future parts
    //that an abort has taken place.
    for (final RestLiTestAttachmentDataSource dataSource : iteratorDataSources)
    {
      Assert.assertTrue(dataSource.dataSourceAborted());
    }
  }

  private void verifyAttachments(final List<MIMETestUtils.SinglePartMIMEFullReaderCallback> singlePartMIMEFullReaderCallbacks,
                                 final List<RestLiTestAttachmentDataSource> dataSources)
  {
    Assert.assertEquals(singlePartMIMEFullReaderCallbacks.size(), dataSources.size());

    for (int i = 0; i < singlePartMIMEFullReaderCallbacks.size(); i++)
    {
      final MIMETestUtils.SinglePartMIMEFullReaderCallback callback = singlePartMIMEFullReaderCallbacks.get(i);
      Assert.assertEquals(callback.getFinishedData(), dataSources.get(i).getPayload());
      Assert.assertEquals(callback.getHeaders().size(), 1);
      Assert.assertEquals(callback.getHeaders().get(RestConstants.HEADER_CONTENT_ID), dataSources.get(i).getAttachmentID());
    }
  }

  private List<RestLiTestAttachmentDataSource> generateTestDataSources()
  {
    final RestLiTestAttachmentDataSource dataSourceA = RestLiTestAttachmentDataSource.createWithRandomPayload("A");
    final RestLiTestAttachmentDataSource dataSourceB = RestLiTestAttachmentDataSource.createWithRandomPayload("B");
    final RestLiTestAttachmentDataSource dataSourceC = RestLiTestAttachmentDataSource.createWithRandomPayload("C");
    final RestLiTestAttachmentDataSource emptyBodySource = new RestLiTestAttachmentDataSource("D", ByteString.empty());

    return new ArrayList<>(Arrays.asList(dataSourceA, dataSourceB, dataSourceC, emptyBodySource));
  }
}