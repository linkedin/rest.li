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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.out;
import static org.testng.Assert.assertEquals;


public class TestBufferChain
{
  private final Map<String, String> _strings = new LinkedHashMap<String, String>();
  private final int[] _bufferSizes = { 17, 19, 23, 29, 31, 37, 41, 43, 47, BufferChain.DEFAULT_BUFFER_SIZE };


  @BeforeClass
  private void initStrings()
  {
    ArrayList<Integer> lengths = new ArrayList<Integer>();
    for (int stringLength = 0; stringLength < 1024; stringLength += 17)
    {
      lengths.add(stringLength);
    }
    for (int stringLength = 1024; stringLength < (Short.MAX_VALUE * 4); stringLength *= 2)
    {
      lengths.add(stringLength);
    }

    for (int stringLength : lengths)
    {
      StringBuilder stringBuilder = new StringBuilder(stringLength);
      char character = 32;
      for (int pos = 0; pos < stringLength; pos++)
      {
        if (character > 16384) character = 32;
        stringBuilder.append(character);
        character += 3;
      }
      String key = "" + stringLength + " character string";
      String value = stringBuilder.toString();
      _strings.put(key, value);
    }
  }

  @Test
  public void testGetUTF8CString() throws Exception
  {
    for (Map.Entry<String, String> entry : _strings.entrySet())
    {
      String key = entry.getKey();
      String value = entry.getValue();
      for (int bufferSize : _bufferSizes)
      {
        // out.println("testing " + key + " with buffer size " + bufferSize);

        byte[] bytesFromString = value.getBytes(Data.UTF_8_CHARSET);
        int bytes = bytesFromString.length + 1;
        byte[] bytesInBuffer = new byte[bytes];
        System.arraycopy(bytesFromString, 0, bytesInBuffer, 0, bytes - 1);
        bytesInBuffer[bytes - 1] = 0;

        // must use package privet constructor break bytes into bufferSize byte buffers internally
        BufferChain bufferChain = new BufferChain(BufferChain.DEFAULT_ORDER, bytesInBuffer, bufferSize);

        bufferChain.rewind();
        byte[] bytesFromBufferChain = new byte[bytes];
        bufferChain.get(bytesFromBufferChain, 0, bytes);
        assertEquals(bytesFromBufferChain, bytesInBuffer);

        bufferChain.rewind();
        String stringWithoutLength = bufferChain.getUtf8CString();
        assertEquals(stringWithoutLength, value);

        bufferChain.rewind();
        String stringGetWithLength = bufferChain.getUtf8CString(bytes);
        assertEquals(stringGetWithLength, value);
      }
    }
  }
}
