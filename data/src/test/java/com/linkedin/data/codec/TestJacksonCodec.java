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

import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests specific to {@link JacksonDataCodec}
 */
public class TestJacksonCodec
{
  /**
   * Test to make sure that field names are not interned into perm gen.
   *
   * @throws IOException
   */
  @Test
  public void testNoStringIntern() throws IOException
  {
    long memToConsume = (long) +8e9;

    int minStringLength = 200000;
    char buf[] = new char[minStringLength];
    for (int i = 0; i < buf.length; i++)
    {
      buf[i] = (char) ('A' + (i % 26));
    }
    String commonPrefix = new String(buf);

    JsonFactory jsonFactory = new JsonFactory();
    JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);

    try
    {
      // make sure intern field names is not enabled
      assertFalse(jsonFactory.isEnabled(JsonParser.Feature.INTERN_FIELD_NAMES));
      consumeMemory(codec, commonPrefix, memToConsume);
    }
    catch (OutOfMemoryError exc)
    {
      // Should run out of heap before perm gen
      assertFalse(exc.getMessage().contains("PermGen") ||
                  !exc.getMessage().contains("GC overhead limit exceeded"));
    }

    try
    {
      // enable intern field names
      jsonFactory.enable(JsonParser.Feature.INTERN_FIELD_NAMES);
      assertTrue(jsonFactory.isEnabled(JsonParser.Feature.INTERN_FIELD_NAMES));
      consumeMemory(codec, commonPrefix, memToConsume);
    }
    catch (OutOfMemoryError exc)
    {
      // Should run out of perm gen before heap
      assertTrue(exc.getMessage().contains("PermGen") ||
                 !exc.getMessage().contains("GC overhead limit exceeded"));
    }
  }

  private void consumeMemory(JacksonDataCodec codec, String commonPrefix, long memToConsume) throws IOException
  {
    String previousKey = "";
    List<DataMap> maps = new ArrayList<DataMap>();

    int count = 0;
    long consumed;

    for (consumed = 0; consumed < memToConsume; consumed += commonPrefix.length(), count++)
    {
      String key = commonPrefix + count;
      assertNotEquals(key, previousKey);
      String json = "{ \"" + commonPrefix + count + "\" : 1 }";
      // System.out.println(json);

      DataMap map = codec.bytesToMap(json.getBytes(Data.UTF_8_CHARSET));

      // If the following is removed, then interned string would be GC from perm gen.
      maps.add(map);

      // System.out.println("count: " + count + ", consumed: " + consumed);
    }

    // System.out.println("count: " + count + ", consumed: " + consumed);
  }
}
