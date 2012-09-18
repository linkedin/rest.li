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
import java.util.ArrayList;
import java.util.Collection;

/**
 * Accumulates data objects returned by a {@link DataIterator}.
 * 
 * @author "Joe Betz<jbetz@linkedin.com>"
 */
public class ValueAccumulator
{
  /**
   * Accumulates the Data objects returned by the {@link DataIterator} into the provided collection.
   * This method mutates the provided collection.
   * 
   * @param it provides the iterator of Data objects to be accumulated.
   * @param accumulator provides the collection that the accumulated Data objects are added to.
   * @return the passed in collection, mutated to include Data objects.
   */
  public static Collection<Object> accumulateValues(DataIterator it, Collection<Object> accumulator)
  {
    for(DataElement element = it.next(); element !=null; element = it.next())
    {
      accumulator.add(element.getValue());
    }
    return accumulator;
  }
  
  /**
   * Accumulates the data objects in the iterator.
   * 
   * @param it provides the iterator of Data objects to be accumulated.
   * @return the passed in collection, mutated to include Data objects.
   */
  public static Collection<Object> accumulateValues(DataIterator it)
  {
    return accumulateValues(it, new ArrayList<Object>());
  }
}
