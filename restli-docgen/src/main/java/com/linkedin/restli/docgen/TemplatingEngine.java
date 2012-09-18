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
import java.util.Map;

/**
 * Interface of templating engine for page rendering.
 *
 * @author dellamag
 */
public interface TemplatingEngine
{
  /**
   * Render a page with the specified template file and data.
   *
   * @param templateName name of the template file to be rendered
   * @param pageModel data to be used when rendering the template page
   * @param out the rendered content are written to this variable
   */
  void render(String templateName, Map<String, Object> pageModel, OutputStream out);
}
