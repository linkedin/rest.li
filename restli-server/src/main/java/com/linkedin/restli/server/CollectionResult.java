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

package com.linkedin.restli.server;

import java.util.List;

import com.linkedin.data.template.RecordTemplate;

public class CollectionResult<T extends RecordTemplate, MD extends RecordTemplate>
{
  private final List<T> _elements;
  private final MD      _metadata;
  private final Integer _total;

  public CollectionResult(final List<T> elements)
  {
    this(elements, null, null);
  }

  public CollectionResult(final List<T> elements, final Integer total)
  {
    this(elements, total, null);
  }

  public CollectionResult(final List<T> elements, final Integer total, final MD metadata)
  {
    _elements = elements;
    _total = total;
    _metadata = metadata;
  }

  public List<T> getElements()
  {
    return _elements;
  }

  public boolean getHasTotal()
  {
    return _total != null;
  }

  public Integer getTotal()
  {
    return _total;
  }

  public MD getMetadata()
  {
    return _metadata;
  }
}
