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


import com.linkedin.multipart.MultiPartMIMEDataSourceIterator;
import com.linkedin.multipart.MultiPartMIMEDataSourceIteratorCallback;
import com.linkedin.multipart.MultiPartMIMEDataSourceWriter;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import com.linkedin.restli.common.attachments.RestLiDataSourceIteratorCallback;

import java.util.Map;
import java.util.TreeMap;


/**
 * Utilities for RestLi attachment streaming. Should only be used by the rest.li framework.
 *
 * @author Karim Vidhani
 */
public class AttachmentUtils
{
  public static final String RESTLI_MULTIPART_SUBTYPE = "related";

  /**
   * Appends the provided {@link com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter} to the provided
   * {@link com.linkedin.multipart.MultiPartMIMEWriter.Builder}. If the provided builder is null, then a new one is created.
   *
   * @param builder the {@link com.linkedin.multipart.MultiPartMIMEWriter.Builder} to append the attachment to.
   * @param streamingAttachment the {@link com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter}
   *                            which will be responsible for writing the data.
   */
  public static void appendSingleAttachmentToBuilder(final MultiPartMIMEWriter.Builder builder,
                                                     final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    builder.appendDataSource(new MultiPartMIMEDataSourceWriter()
    {
      @Override
      public Map<String, String> dataSourceHeaders()
      {
        final Map<String, String> dataSourceHeaders = new TreeMap<>();
        dataSourceHeaders.put(RestConstants.HEADER_CONTENT_ID, streamingAttachment.getAttachmentID());
        return dataSourceHeaders;
      }

      @Override
      public void onInit(WriteHandle wh)
      {
        streamingAttachment.onInit(wh);
      }

      @Override
      public void onWritePossible()
      {
        streamingAttachment.onWritePossible();
      }

      @Override
      public void onAbort(Throwable e)
      {
        streamingAttachment.onAbort(e);
      }
    });
  }

  /**
   * Appends the provided {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator} to the provided
   * {@link com.linkedin.multipart.MultiPartMIMEWriter.Builder}. If the provided builder is null, then a new one is created.
   *
   * @param builder the {@link com.linkedin.multipart.MultiPartMIMEWriter.Builder} to append the data source iterator to.
   * @param restLiDataSourceIterator the {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator}
   *                                 which will be responsible for writing the data for each attachment.
   */
  public static void appendMultipleAttachmentsToBuilder(final MultiPartMIMEWriter.Builder builder,
                                                        final RestLiDataSourceIterator restLiDataSourceIterator)
  {
    builder.appendDataSourceIterator(new MultiPartMIMEDataSourceIterator()
    {
      @Override
      public void abandonAllDataSources()
      {
        restLiDataSourceIterator.abandonAllDataSources();
      }

      @Override
      public void registerDataSourceReaderCallback(MultiPartMIMEDataSourceIteratorCallback callback)
      {
        restLiDataSourceIterator.registerDataSourceReaderCallback(new RestLiDataSourceIteratorCallback()
        {
          @Override
          public void onNewDataSourceWriter(RestLiAttachmentDataSourceWriter dataSourceWriter)
          {
            callback.onNewDataSource(new MultiPartMIMEDataSourceWriter()
            {
              @Override
              public Map<String, String> dataSourceHeaders()
              {
                final Map<String, String> dataSourceHeaders = new TreeMap<>();
                dataSourceHeaders.put(RestConstants.HEADER_CONTENT_ID, dataSourceWriter.getAttachmentID());
                return dataSourceHeaders;
              }

              @Override
              public void onInit(WriteHandle wh)
              {
                dataSourceWriter.onInit(wh);
              }

              @Override
              public void onWritePossible()
              {
                dataSourceWriter.onWritePossible();
              }

              @Override
              public void onAbort(Throwable e)
              {
                dataSourceWriter.onAbort(e);
              }
            });
          }

          @Override
          public void onFinished()
          {
            callback.onFinished();
          }

          @Override
          public void onAbandonComplete()
          {
            callback.onAbandonComplete();
          }

          @Override
          public void onStreamError(Throwable throwable)
          {
            callback.onStreamError(throwable);
          }
        });
      }
    });
  }

  /**
   * Create a {@link com.linkedin.multipart.MultiPartMIMEWriter} using the specified parameters.
   *
   * @param firstPartWriter Represents the contents of the first part, the json/pson portion.
   * @param firstPartContentType The content type of the first part, i.e json or pson.
   * @param streamingAttachments Any developer provided attachments to be added onto the outgoing request.
   */
  public static MultiPartMIMEWriter createMultiPartMIMEWriter(final Writer firstPartWriter,
                                                              final String firstPartContentType,
                                                              final MultiPartMIMEWriter.Builder streamingAttachments)
  {
    //We know that streamingAttachments is non-null at this point.
    streamingAttachments.prependDataSource(new MultiPartMIMEDataSourceWriter()
    {
      @Override
      public Map<String, String> dataSourceHeaders()
      {
        final Map<String, String> metadataHeaders = new TreeMap<>();
        metadataHeaders.put(RestConstants.HEADER_CONTENT_TYPE, firstPartContentType);
        return metadataHeaders;
      }

      @Override
      public void onInit(WriteHandle wh)
      {
        firstPartWriter.onInit(wh);
      }

      @Override
      public void onWritePossible()
      {
        firstPartWriter.onWritePossible();
      }

      @Override
      public void onAbort(Throwable e)
      {
        firstPartWriter.onAbort(e);
      }
    });

    return streamingAttachments.build();
  }
}
