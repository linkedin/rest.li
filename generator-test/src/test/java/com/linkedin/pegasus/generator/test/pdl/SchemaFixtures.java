/*
 Copyright 2015 Coursera Inc.

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

package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.data.ByteString;
import com.linkedin.data.schema.BooleanDataSchema;
import com.linkedin.data.schema.BytesDataSchema;
import com.linkedin.data.schema.DoubleDataSchema;
import com.linkedin.data.schema.FloatDataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.LongDataSchema;
import com.linkedin.data.schema.StringDataSchema;


public class SchemaFixtures
{
  public static ByteString bytes1 = ByteString.copy(new byte[]{0x0, 0x1, 0x2});
  public static ByteString bytes2 = ByteString.copy(new byte[]{0x3, 0x4, 0x5});
  public static ByteString bytes3 = ByteString.copy(new byte[]{0x6, 0x7, 0x8});

  public static ByteString bytesFixed8 = ByteString.copy(new byte[]{0, 1, 2, 3, 4, 5, 6, 7});
}
