/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.resources.unstructuredData.AssociationUnstructuredDataResourceTemplate;

import static com.linkedin.restli.common.RestConstants.HEADER_CONTENT_DISPOSITION;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.CONTENT_DISPOSITION_VALUE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.respondBadUnstructuredData;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.respondGoodUnstructuredData;


/**
 * This resource models an association resource that produces unstructured data entities as results.
 */
@RestLiAssociation(
  name = "greetingAssociationUnstructuredData",
  namespace = "com.linkedin.restli.examples.greetings.client",
  assocKeys = {
    @Key(name = "src", type = String.class),
    @Key(name = "dest", type = String.class)
  }
)
public class GreetingAssociationUnstructuredDataResource extends AssociationUnstructuredDataResourceTemplate
{
  @Override
  public void get(CompoundKey key, @UnstructuredDataWriterParam UnstructuredDataWriter writer)
  {
    switch (key.getPartAsString("src"))
    {
      case "good":
        respondGoodUnstructuredData(writer);
        break;
      case "goodInline":
        getContext().setResponseHeader(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE);
        respondGoodUnstructuredData(writer);
        break;
      case "missingHeaders":
        respondBadUnstructuredData(writer);
        break;
      case "exception":
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                         "internal service exception");
      default:
        throw new RestLiServiceException(HttpStatus.S_503_SERVICE_UNAVAILABLE,
                                         "unexpected unstructured data key, something wrong with the test.");
    }
  }
}