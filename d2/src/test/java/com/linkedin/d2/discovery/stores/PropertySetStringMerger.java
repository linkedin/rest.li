/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores;

import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PropertySetStringMerger implements ZooKeeperPropertyMerger<Set<String>>
{
  @Override
  public Set<String> merge(String propertyName, Collection<Set<String>> propertiesToMerge)
  {

    Set<String> mergedLists = new HashSet<>();
    for (Set<String> strings : propertiesToMerge)
    {
      mergedLists.addAll(strings);
    }
    if (mergedLists.size() > 0)
    {
      return mergedLists;
    }
    else
    {
      return null;
    }
  }

  @Override
  public String unmerge(String propertyName,
                        Set<String> toDelete,
                        Map<String, Set<String>> propertiesToMerge)
  {
    for (Map.Entry<String, Set<String>> property : propertiesToMerge.entrySet())
    {
      if (toDelete.containsAll(property.getValue()))
      {
        return property.getKey();
      }
    }
    return null;
  }
}
