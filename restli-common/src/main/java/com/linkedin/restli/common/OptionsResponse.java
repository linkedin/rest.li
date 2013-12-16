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
package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents a OPTIONS response.
 *
 * @author jbetz
 */
public class OptionsResponse
{
  private final Map<String, ResourceSchema> _resourceSchemas;
  private final Map<String, DataSchema> _dataSchemas;

  public OptionsResponse(Map<String, ResourceSchema> resourceSchemas, Map<String, DataSchema> dataSchemas)
  {
    _resourceSchemas = resourceSchemas;
    _dataSchemas = dataSchemas;
  }

  public Map<String, DataSchema> getDataSchemas() throws IOException
  {
    return Collections.unmodifiableMap(_dataSchemas);
  }

  public Map<String, ResourceSchema> getResourceSchemas()
  {
    return Collections.unmodifiableMap(_resourceSchemas);
  }
}
