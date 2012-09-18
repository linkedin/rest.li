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

package com.linkedin.d2.balancer.util.partitions;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPartitionAccessor implements PartitionAccessor
{
  final private Pattern _pattern;
  final private int     _maxPartitionId;

  public AbstractPartitionAccessor(String pattenStr, int maxPartitionId)
  {
    _pattern = Pattern.compile(pattenStr);
    _maxPartitionId = maxPartitionId;
  }

  @Override
  public int getPartitionId(URI uri) throws PartitionAccessException
  {
    final String uriString = uri.toString();
    final Matcher matcher = _pattern.matcher(uriString);
    if (matcher.find())
    {
      // When people supply the regex, ideally there should be only on matching group (wrapped with '()')
      // which d2 would use as the partitioning key
      final String key = matcher.group(matcher.groupCount());
      return getPartitionId(key);
    }
    else
    {
      throw new PartitionAccessException("Pattern: " + _pattern.toString()
          + " does not match anything in request URI: " + uriString);
    }
  }

  @Override
  public int getMaxPartitionId()
  {
    return _maxPartitionId;
  }
}
