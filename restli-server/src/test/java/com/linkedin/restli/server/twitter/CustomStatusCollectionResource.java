/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.twitter;


import com.linkedin.restli.server.CustomFixedLengthStringRef;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.custom.types.CustomFixedLengthString;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CollectionResource containing all statuses with CustomFixedLengthString as key type
 *
 * @author Min Chen
 */
@RestLiCollection(name = "custom_status",
    keyTyperefClass = CustomFixedLengthStringRef.class)
public class CustomStatusCollectionResource extends CollectionResourceTemplate<CustomFixedLengthString, Status>
{
  /**
   * Gets a single status resource
   */
  @Override
  public Status get(CustomFixedLengthString key)
  {
    return null;
  }

  /**
   * Gets a batch of statuses
   */
  @Override
  public Map<CustomFixedLengthString, Status> batchGet(Set<CustomFixedLengthString> ids)
  {
    return null;
  }

  /**
   * Keyword search for statuses
   *
   * @param keywords keyword to search for
   */
  @Finder("search")
  public List<Status> search(@QueryParam(value="keywords",typeref=CustomFixedLengthStringRef.class) CustomFixedLengthString keywords)
  {
    return null;
  }
}
