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
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.custom.types.CustomFixedLengthString;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;


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
}
