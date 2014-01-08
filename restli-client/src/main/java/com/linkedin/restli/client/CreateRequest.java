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

package com.linkedin.restli.client;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;
import java.util.Map;


/**
 * A request for writing a resource.
 *
 * @author Eran Leshem
 */
public class CreateRequest<T extends RecordTemplate>
        extends Request<EmptyRecord>
{
  CreateRequest(T input,
                Map<String, String> headers,
                ResourceSpec resourceSpec,
                Map<String, Object> queryParams,
                String baseUriTemplate,
                Map<String, Object> pathKeys,
                RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.CREATE,
          input,
          headers,
          new EmptyResponseDecoder(),
          resourceSpec,
          queryParams,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions);
  }
}
