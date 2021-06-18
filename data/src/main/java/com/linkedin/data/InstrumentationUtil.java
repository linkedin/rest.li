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

package com.linkedin.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for data instrumentation
 *
 * @author Eran Leshem
 */
public class InstrumentationUtil
{
  private InstrumentationUtil()
  {
  }

  public static void emitInstrumentationData(StringBuilder key,
                                             Object object,
                                             Integer timesAccessed,
                                             Map<String, Map<String, Object>> instrumentedData)
  {
    Map<String, Object> attributeMap = new HashMap<>(2);
    attributeMap.put(Instrumentable.VALUE, String.valueOf(object));
    attributeMap.put(Instrumentable.TIMES_ACCESSED, timesAccessed);
    instrumentedData.put(key.toString(), attributeMap);
  }
}
