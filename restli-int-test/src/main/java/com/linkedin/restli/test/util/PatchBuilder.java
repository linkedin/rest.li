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

package com.linkedin.restli.test.util;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.internal.server.methods.arguments.ArgumentBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for building patch requests. Used for unit testing.
 *
 * @author Soojung Ha
 */
public class PatchBuilder
{
  @SuppressWarnings("unchecked")
  public static <T extends RecordTemplate> PatchRequest<T> buildPatchFromString(String patch)
  {
    PatchRequest<T> patchRequest = null;
    try
    {
      RestRequest restRequest = new RestRequestBuilder(new URI("/test")).setEntity(patch.getBytes()).build();
      patchRequest = (PatchRequest<T>) ArgumentBuilder.extractEntity(restRequest, PatchRequest.class);
    }
    catch (URISyntaxException e)
    {
    }
    return patchRequest;
  }
}