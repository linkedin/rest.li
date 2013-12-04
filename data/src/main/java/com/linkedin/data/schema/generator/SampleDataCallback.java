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

package com.linkedin.data.schema.generator;


import com.linkedin.data.ByteString;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;


/**
 * Callback interface for generating sample data from various given criteria.
 *
 * @author Keren Jin
 */
public interface SampleDataCallback
{
  /**
   * @return sample data for {@link com.linkedin.data.schema.BooleanDataSchema}
   */
  public boolean getBoolean(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.IntegerDataSchema}
   */
  public int getInteger(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.LongDataSchema}
   */
  public long getLong(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.FloatDataSchema}
   */
  public float getFloat(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.DoubleDataSchema}
   */
  public double getDouble(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.BytesDataSchema}
   */
  public ByteString getBytes(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.StringDataSchema}
   */
  public String getString(String fieldName);

  /**
   * @return sample data for {@link com.linkedin.data.schema.FixedDataSchema}
   */
  public ByteString getFixed(String fieldName, FixedDataSchema schema);

  /**
   * @return sample data for {@link com.linkedin.data.schema.EnumDataSchema}
   */
  public String getEnum(String fieldName, EnumDataSchema schema);
}
