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
    String keyName = "testKey";
    String json = "{ \"" + keyName + "\" : 1 }";
    byte[] jsonAsBytes = json.getBytes(Data.UTF_8_CHARSET);

    {
      JsonFactory jsonFactory = new JsonFactory();
      JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // make sure intern field names is not enabled
      assertFalse(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      DataMap map = codec.bytesToMap(jsonAsBytes);
      String key = map.keySet().iterator().next();
      assertNotSame(key, keyName);
    }

    {
      JsonFactory jsonFactory = new JsonFactory();
      JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // enable intern field names
      jsonFactory.enable(JsonFactory.Feature.INTERN_FIELD_NAMES);
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      DataMap map = codec.bytesToMap(jsonAsBytes);
      String key = map.keySet().iterator().next();
      assertSame(key, keyName);
    }
  }
}
