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


import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.ServerResourceContext;

import java.io.OutputStream;


/**
 * This is used for sending a binary unstructured data service response. An instance of this class should be
 * provided to developer by Rest.li as part of the UnstructuredData-based resource interface. UnstructuredDataWriter provides
 * setters for required response headers and an {@link OutputStream} for writing the unstructured data content.
 *
 * <p>Example usage 1:</p>
 * <pre><code>
 *    unstructuredDataWriter.setContentType("application/pdf");   // Set response headers first (recommended)
 *    byte[] profilePDF = fetchProfilePDF();          // Fetch the unstructured data content
 *    unstructuredDataWriter.getOutputStream().write(profilePDF); // Write unstructured data content
 *    return;                                         // Return after full content is written
 * </code></pre>
 *
 * <p>Example usage 2:</p>
 * <pre><code>
 *   unstructuredDataWriter.setContentType("application/pdf");    // Set response headers first (recommended)
 *   ReportGenerator rg = reportGenerator(rawSrc);    // Some data producer that generates the unstructured data from source
 *   rg.setOutputStream(unstructuredDataWriter.getOutputStream());// The data producer writes to an OutputStream
 *   rg.start();                                      // Start writing (must be blocking)
 *   return;                                          // Return after full content is written
 * </code></pre>
 *
 * It is recommended to set response headers before writing to the {@link #getOutputStream()}, this allows
 * future optimization by the framework.
 */
public class UnstructuredDataWriter
{
  private OutputStream _outputStream;
  private ServerResourceContext _resourceContext;

  public UnstructuredDataWriter(OutputStream outputStream, ServerResourceContext resourceContext)
  {
    _outputStream = outputStream;
    _resourceContext = resourceContext;
    _resourceContext.setUnstructuredDataWriter(this);
  }

  /**
   * Set the MIME content-type of the unstructured data.
   */
  public void setContentType(String contentType)
  {
    _resourceContext.setResponseHeader(RestConstants.HEADER_CONTENT_TYPE, contentType);
  }

  /**
   * Return the underlying output stream for writing unstructured data content.
   */
  public OutputStream getOutputStream()
  {
    return _outputStream;
  }
}
