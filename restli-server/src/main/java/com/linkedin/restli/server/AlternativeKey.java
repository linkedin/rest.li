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


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.KeyCoercer;


/**
 * Class representing an alternative key.
 * Contains the type of the alternative key, the schema of the alternative key, and a coercer for converting between
 * the alternative and primary key formats.
 *
 * @param <T> Alternative key type.
 * @param <K> Primary key type.
 */
public class AlternativeKey<T, K>
{
  private KeyCoercer<T, K> _keyCoercer;
  private Class<T> _type;
  private DataSchema _dataSchema;

  /**
   * @param keyCoercer Coercer that can convert between alternative key and primary key formats.
   * @param type The {@link java.lang.Class} of the alternative key.
   * @param dataSchema The {@link com.linkedin.data.schema.DataSchema} of the alternative key type.
   */
  public AlternativeKey(KeyCoercer<T, K> keyCoercer,
                        Class<T> type,
                        DataSchema dataSchema)
  {
    _keyCoercer = keyCoercer;
    _type = type;
    _dataSchema = dataSchema;
  }

  public KeyCoercer<T, K> getKeyCoercer()
  {
    return _keyCoercer;
  }

  public Class<T> getType()
  {
    return _type;
  }

  public DataSchema getDataSchema()
  {
    return _dataSchema;
  }
}
