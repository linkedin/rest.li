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

package com.linkedin.restli.docgen;

import java.io.OutputStream;
import java.net.URI;
import java.util.function.Function;


/**
 * Interface of renderer for documentation generation.
 *
 * @author Keren Jin
 */
public interface RestLiDocumentationRenderer
{
  /**
   * Supported documentation format types.
   */
  enum DocumentationFormat
  {
    HTML,
    JSON
  }

  /**
   * Render the homepage of documentation. The homepage is accessed at the root of the documentation URL path.
   * @param out The function will write rendered content to this stream
   */
  void renderHome(OutputStream out);

  default void renderHome(RenderContext context)
  {
    renderHome(context.getOutputStream());
  }

  /**
   * Render the homepage of documentation for resources.
   * @param out The function will write rendered content to this stream
   */
  void renderResourceHome(OutputStream out);

  default void renderResourceHome(RenderContext context)
  {
    renderResourceHome(context.getOutputStream());
  }

  /**
   * Render documentation of the given resource.
   * @param resourceName name of the resource to render
   * @param out The function will write rendered content to this stream
   */
  void renderResource(String resourceName, OutputStream out);

  default void renderResource(String resourceName, RenderContext context)
  {
    renderResource(resourceName, context.getOutputStream());
  }

  /**
   * Render the homepage of documentation for data models.
   * @param out The function will write rendered content to this stream
   */
  void renderDataModelHome(OutputStream out);

  default void renderDataModelHome(RenderContext context)
  {
    renderDataModelHome(context.getOutputStream());
  }

  /**
   * Render documentation of the given data model.
   * @param dataModelName name of the data model to render
   * @param out The function will write rendered content to this stream
   */
  void renderDataModel(String dataModelName, OutputStream out);

  default void renderDataModel(String dataModelName, RenderContext context)
  {
    renderDataModel(dataModelName, context.getOutputStream());
  }

  /**
   * Handler for runtime exception in the documentation renderer. When return false,
   * out parameter should be not changed.
   * @param e The exception to be handled
   * @param out The function will write rendered content to this stream
   * @return is the exception handled
   */
  boolean handleException(RuntimeException e, OutputStream out);

  default boolean handleException(RuntimeException e, RenderContext context)
  {
    return handleException(e, context.getOutputStream());
  }

  /**
   * @return MIME type of the rendered content. All render function must be consistent to this MIME type
   */
  String getMIMEType();

  /**
   * Set the uri provider to get the documentation in other formats. Can be used to include links to alternate formats
   * in the generated documentation.
   * @param uriProvider Provides the RUI to fetch documentation in the other formats.
   */
  default void setFormatUriProvider(Function<DocumentationFormat, URI> uriProvider)
  {
  }
}
