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

package com.linkedin.data.codec;


import com.fasterxml.jackson.core.JsonFactory;
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests specific to {@link JacksonDataCodec}
 */
public class TestJacksonCodec
{
  /**
   * Test to make sure that field names are not interned.
   *
   * @throws IOException
   */
  @Test
  public void testNoStringIntern() throws IOException
  {
    final String keyName = "testKey";
    final String json = "{ \"" + keyName + "\" : 1 }";
    final byte[] jsonAsBytes = json.getBytes(Data.UTF_8_CHARSET);

    {
      final JsonFactory jsonFactory = new JsonFactory();
      final JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // make sure intern field names is not enabled
      assertFalse(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      final DataMap map = codec.bytesToMap(jsonAsBytes);
      final String key = map.keySet().iterator().next();
      assertNotSame(key, keyName);
    }

    {
      final JsonFactory jsonFactory = new JsonFactory();
      final JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // enable intern field names
      jsonFactory.enable(JsonFactory.Feature.INTERN_FIELD_NAMES);
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      final DataMap map = codec.bytesToMap(jsonAsBytes);
      final String key = map.keySet().iterator().next();
      assertSame(key, keyName);
    }
  }

  /**
   * Prior to version 2.4.3, Jackson could not handle map keys >= 262146 bytes, if the data source is byte array.
   * The issue is resolved in https://github.com/FasterXML/jackson-core/issues/152
   */
  @Test
  public void testLongKeyFromByteSource() throws IOException
  {
    final StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\"");
    for (int i = 0; i < 43691; ++i)
    {
      jsonBuilder.append("6_byte");
    }
    jsonBuilder.append("\":0}");

    final JacksonDataCodec codec = new JacksonDataCodec();
    final DataMap map = codec.bytesToMap(jsonBuilder.toString().getBytes());
    Assert.assertEquals(map.keySet().iterator().next().length(), 262146);
  }
}
