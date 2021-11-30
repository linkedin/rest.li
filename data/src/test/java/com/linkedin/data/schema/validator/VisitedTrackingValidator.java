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

package com.linkedin.data.schema.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisitedTrackingValidator implements Validator
{
  private final List<String> _visited = new ArrayList<>();
  private final Validator _nextValidator;

  public VisitedTrackingValidator(Validator nextValidator)
  {
    _nextValidator = nextValidator;
  }

  @Override
  public void validate(ValidatorContext ctx)
  {
    _visited.add(ctx.dataElement().pathAsString());
    if (_nextValidator != null)
    {
      _nextValidator.validate(ctx);
    }
  }

  public List<String> getVisited()
  {
    return _visited;
  }

  public Set<String> getVisitedMoreThanOnce()
  {
    Set<String> visitedMoreThanOnce = new HashSet<>();
    Set<String> visitedSet = new HashSet<>();
    for (String path : _visited)
    {
      boolean added = visitedSet.add(path);
      if (added == false)
        visitedMoreThanOnce.add(path);
    }
    return visitedMoreThanOnce;
  }
}
