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

package com.linkedin.restli.server;


import com.linkedin.data.ByteString;
import com.linkedin.java.util.concurrent.Flow;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.restli.common.streaming.FlowBridge;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSourceIterator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Test for {@link RestLiResponseAttachments}
 *
 * @author Karim Vidhani
 */
public class TestRestLiResponseAttachments
{
  @Test
  public void testRestLiResponseAttachments()
  {
    //In this test we simply add a few attachments and verify the size of the resulting MultiPartMIMEWriter.
    //More detailed tests can be found in TestAttachmentUtils.

    final RestLiResponseAttachments emptyAttachments = new RestLiResponseAttachments.Builder().build();
    Assert.assertEquals(emptyAttachments.getMultiPartMimeWriterBuilder().getCurrentSize(), 0);

    //For multiple data attachments
    final RestLiTestAttachmentDataSource dataSourceA =
        new RestLiTestAttachmentDataSource("A", ByteString.copyString("partA", Charset.defaultCharset()));
    final RestLiTestAttachmentDataSource dataSourceB =
        new RestLiTestAttachmentDataSource("B", ByteString.copyString("partB", Charset.defaultCharset()));
    final RestLiTestAttachmentDataSource dataSourceC =
        new RestLiTestAttachmentDataSource("C", ByteString.copyString("partC", Charset.defaultCharset()));

    final RestLiResponseAttachments.Builder multipleAttachmentsBuilder = new RestLiResponseAttachments.Builder();
    multipleAttachmentsBuilder.appendSingleAttachment(dataSourceA);

    final RestLiTestAttachmentDataSourceIterator dataSourceIterator = new RestLiTestAttachmentDataSourceIterator(
        Arrays.asList(dataSourceB, dataSourceC), new IllegalArgumentException());
    multipleAttachmentsBuilder.appendMultipleAttachments(dataSourceIterator);

    RestLiResponseAttachments attachments = multipleAttachmentsBuilder.build();
    Assert.assertEquals(attachments.getMultiPartMimeWriterBuilder().getCurrentSize(), 2);
    Assert.assertNull(attachments.getUnstructuredDataWriter());
    Assert.assertNull(attachments.getUnstructuredDataReactiveResult());
  }

  @Test
  public void testRestLiResponseAttachmentsForUnstructuredData()
    throws RestLiSyntaxException
  {
    UnstructuredDataWriter unstructuredDataWriter = new UnstructuredDataWriter(new ByteArrayOutputStream(), new ResourceContextImpl());
    final RestLiResponseAttachments attachments = new RestLiResponseAttachments.Builder().appendUnstructuredDataWriter(unstructuredDataWriter).build();
    Assert.assertEquals(attachments.getUnstructuredDataWriter(), unstructuredDataWriter);
  }

  @Test
  public void testRestLiResponseAttachmentsForPublisher()
    throws RestLiSyntaxException
  {
    ByteStringWriter writer = new ByteStringWriter(ByteString.empty());
    Flow.Publisher<ByteString> publisher = FlowBridge.toPublisher(writer);
    UnstructuredDataReactiveResult publisherWrapper = new UnstructuredDataReactiveResult(publisher, "contentType");
    final RestLiResponseAttachments attachments = new RestLiResponseAttachments.Builder().appendUnstructuredDataReactiveResult(publisherWrapper).build();
    Assert.assertEquals(attachments.getUnstructuredDataReactiveResult(), publisherWrapper);
    Assert.assertEquals(attachments.getUnstructuredDataReactiveResult().getPublisher(), publisher);
  }
}