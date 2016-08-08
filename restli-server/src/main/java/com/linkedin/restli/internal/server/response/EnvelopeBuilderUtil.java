/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ResourceMethod;
import java.util.Collections;


class EnvelopeBuilderUtil
{
  public static RestLiResponseEnvelope buildBlankResponseEnvelope(ResourceMethod resourceMethod,
                                                                  RestLiResponseDataImpl responseData)
  {
    switch (resourceMethod)
    {
      case GET:
        return new GetResponseEnvelope(new EmptyRecord(), responseData);
      case CREATE:
        return new CreateResponseEnvelope(new EmptyRecord(), responseData);
      case ACTION:
        return new ActionResponseEnvelope(new EmptyRecord(), responseData);
      case GET_ALL:
        return new GetAllResponseEnvelope(Collections.<RecordTemplate>emptyList(), null, new EmptyRecord(), responseData);
      case FINDER:
        return new FinderResponseEnvelope(Collections.<RecordTemplate>emptyList(), null, new EmptyRecord(), responseData);
      case BATCH_CREATE:
        return new BatchCreateResponseEnvelope(Collections.<BatchCreateResponseEnvelope.CollectionCreateResponseItem>emptyList(), responseData);
      case BATCH_GET:
        return new BatchGetResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), responseData);
      case BATCH_UPDATE:
        return new BatchUpdateResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), responseData);
      case BATCH_PARTIAL_UPDATE:
        return new BatchPartialUpdateResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), responseData);
      case BATCH_DELETE:
        return new BatchDeleteResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), responseData);
      case PARTIAL_UPDATE:
        return new PartialUpdateResponseEnvelope(responseData);
      case UPDATE:
        return new UpdateResponseEnvelope(responseData);
      case DELETE:
        return new DeleteResponseEnvelope(responseData);
      case OPTIONS:
        return new OptionsResponseEnvelope(responseData);
      default:
        throw new IllegalStateException();
    }
  }
}
