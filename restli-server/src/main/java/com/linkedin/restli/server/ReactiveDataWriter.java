/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.server;


import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.util.ArgumentUtil;


/**
 * This class represents an unstructured data response that can be streamed in a reactive manner. An instance of this
 * class should be provided by resource developer and returned from the resource method.
 *
 * <p>Example usage:</p>
 * <pre><code>
 *   Writer fileWriter = new FileWriter("/tmp/file_to_send");        // Use an existing or implement your own {@link Writer} that can fetch source data reactively
 *   ReactiveDataWriter writer = new ReactiveDataWriter(fileWriter); // Create and setup a {@link ReactiveDataWriter}
 *   writer.setContentType("application/pdf");                       // Set the response MIME type
 *   return writer;                                                  // Return the writer and send the response
 * </code></pre>
 */
public class ReactiveDataWriter
{
  private Writer _writer;
  private String _contentType;

  /**
   * Create a response writer with an underlying reactive streaming {@link Writer}
   */
  public ReactiveDataWriter(Writer writer)
  {
    ArgumentUtil.notNull(writer, "Reactive data writer");
    _writer = writer;
  }

  /**
   * Set the MIME content-type of the unstructured data.
   */
  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }

  /**
   * Internal Only. Fetch the underlying {@link Writer} for the unstructured data.
   */
  Writer getWriter()
  {
    return _writer;
  }

  /**
   * Internal Only. Fetch the MIME content type of the unstructured data.
   */
  String getContentType()
  {
    return _contentType;
  }
}