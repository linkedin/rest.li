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

package com.linkedin.restli.server;


import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import com.linkedin.restli.internal.common.AttachmentUtils;


/**
 * Represents an ordered list of attachments to be sent in a request or sent back in a response. Attachments may only
 * be either a {@link com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter} or a
 * {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator}.
 *
 * Upon construction this list of attachments may be set via
 * {@link ResourceContext#setResponseAttachments(com.linkedin.restli.server.RestLiResponseAttachments)}.
 *
 * NOTE: If an exception occurs after response attachments have been set (such as an exception thrown by the resource
 * method, an exception in any rest.li response filters or an exception in the rest.li framework while sending the
 * response), then every attachment in this list of attachments will be told to to abort via
 * {@link com.linkedin.r2.message.stream.entitystream.Writer#onAbort(java.lang.Throwable)}.
 *
 * @author Karim Vidhani
 */
public class RestLiResponseAttachments
{
  private final MultiPartMIMEWriter.Builder _multiPartMimeWriterBuilder;
  private UnstructuredDataWriter _unstructuredDataWriter;
  private ReactiveDataWriter _reactiveDataWriter;

  /**
   * Builder to create an instance of RestLiResponseAttachments.
   */
  public static class Builder
  {
    private final MultiPartMIMEWriter.Builder _multiPartMimeWriterBuilder;
    private UnstructuredDataWriter _unstructuredDataWriter;
    private ReactiveDataWriter _reactiveDataWriter;

    /**
     * Create a RestLiResponseAttachments Builder.
     *
     * @return the builder to continue building.
     */
    public Builder()
    {
      _multiPartMimeWriterBuilder = new MultiPartMIMEWriter.Builder();
    }

    /**
     * Append a {@link com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter} to be placed as an attachment.
     *
     * @param dataSource the data source to be added.
     * @return the builder to continue building.
     */
    public Builder appendSingleAttachment(final RestLiAttachmentDataSourceWriter dataSource)
    {
      AttachmentUtils.appendSingleAttachmentToBuilder(_multiPartMimeWriterBuilder, dataSource);
      return this;
    }

    /**
     * Append a {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator} to be used as a data source
     * within the newly constructed attachment list. All the individual attachments produced from the
     * {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator} will be chained and placed as attachments in the new
     * attachment list.
     *
     * @param dataSourceIterator
     * @return the builder to continue building.
     */
    public Builder appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
    {
      AttachmentUtils.appendMultipleAttachmentsToBuilder(_multiPartMimeWriterBuilder, dataSourceIterator);
      return this;
    }

    /**
     * Append a {@link UnstructuredDataWriter} to be used for sending a unstructured data response.
     *
     * @param dataSourceIterator
     * @return the builder to continue building.
     */
    public Builder appendUnstructuredDataWriter(final UnstructuredDataWriter unstructuredDataWriter)
    {
      _unstructuredDataWriter = unstructuredDataWriter;
      return this;
    }

    /**
     * Append a {@link ReactiveDataWriter} to be used for streaming a unstructured data response reactively.
     *
     * @param dataSourceIterator
     * @return the builder to continue building.
     */
    public Builder appendReactiveDataWriter(final ReactiveDataWriter reactiveDataWriter)
    {
      _reactiveDataWriter = reactiveDataWriter;
      return this;
    }

    /**
     * Construct and return the newly formed {@link RestLiResponseAttachments}.
     * @return the fully constructed {@link RestLiResponseAttachments}.
     */
    public RestLiResponseAttachments build()
    {
      return new RestLiResponseAttachments(this);
    }
  }

  private RestLiResponseAttachments(final RestLiResponseAttachments.Builder builder)
  {
    _multiPartMimeWriterBuilder = builder._multiPartMimeWriterBuilder;
    _unstructuredDataWriter = builder._unstructuredDataWriter;
    _reactiveDataWriter = builder._reactiveDataWriter;
  }

  /**
   * Internal use only for rest.li framework.
   *
   * Returns the {@link com.linkedin.multipart.MultiPartMIMEWriter.Builder} representing the attachments added
   * thus far. Returns null if this {@link RestLiResponseAttachments} carries a unstructured data reponse.
   */
  public MultiPartMIMEWriter.Builder getMultiPartMimeWriterBuilder()
  {
    return _multiPartMimeWriterBuilder;
  }

  /**
   * Internal use only for rest.li framework.
   *
   * Returns the {@link UnstructuredDataWriter} representing a unstructured data response. Returns null if this
   * {@link RestLiResponseAttachments} carries Rest.li attachments.
   */
  public UnstructuredDataWriter getUnstructuredDataWriter()
  {
    return _unstructuredDataWriter;
  }

  /**
   * Internal use only for rest.li framework.
   */
  public ReactiveDataWriter getReactiveDataWriter()
  {
    return _reactiveDataWriter;
  }

}