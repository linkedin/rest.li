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

package com.linkedin.data.it;

import com.linkedin.data.element.DataElement;

/**
 * Counts Data objects returned by a {@link DataIterator}.
 * 
 * @author "Joe Betz<jbetz@linkedin.com>"
 */
public class Counter
{
  /**
   * Counts the Data objects returned by the {@link DataIterator}.
   * 
   * @param it provides the iterator of Data objects to be counted.
   * @return the count of Data objects.
   */
  public static int count(DataIterator it)
  {
    @SuppressWarnings("unused") // used in while loop
    int count = 0;
    for(DataElement element = it.next(); element !=null; element = it.next())
    {
      count++;
    }
    return count;
  }
}
