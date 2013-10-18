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

package com.linkedin.restli.tools.snapshot.check;


import com.linkedin.data.DataMap;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class Snapshot extends AbstractSnapshot
{
  /**
   * Create a Snapshot based on the given {@link InputStream}
   * @param inputStream an input stream that represents a {@link DataMap} with two fields: "models", which
   * @throws IOException if the inputStream cannot be parsed as a {@link DataMap} or {@link String}.
   */
  public Snapshot(InputStream inputStream) throws IOException
  {
    DataMap data = _dataCodec.readMap(inputStream);
    _models = parseModels(data.getDataList(MODELS_KEY));
    _resourceSchema = parseSchema(data.getDataMap(SCHEMA_KEY));
  }
}
