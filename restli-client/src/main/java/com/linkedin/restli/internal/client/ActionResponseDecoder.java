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

package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.FieldDef;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ProtocolVersion;

import java.util.Map;


/**
 * Converts a raw RestResponse to a type-bound action response.
 *
 * @param <T> response type
 *
 * @author Eran Leshem
 */
public class ActionResponseDecoder<T> extends RestResponseDecoder<T>
{
  private final FieldDef<T> _returnFieldDef;
  private final RecordDataSchema _recordDataSchema;

  /**
   * @param returnFieldDef the {@link FieldDef} of the response.
   *                       If this is null, it is assumed that the entity class of the response is void.
   * @param schema the {@link RecordDataSchema} of the response.
   */
  public ActionResponseDecoder(FieldDef<T> returnFieldDef, RecordDataSchema schema)
  {
    _returnFieldDef = returnFieldDef;
    _recordDataSchema = schema;
  }

  /**
   * @return the type of the response {@link FieldDef},
   * or {@code Void.class} if the response {@link FieldDef} is null.
   */
  @Override
  public Class<?> getEntityClass()
  {
    return _returnFieldDef == null ? Void.class : _returnFieldDef.getType();
  }

  @Override
  protected T wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    return new ActionResponse<T>(dataMap, _returnFieldDef, _recordDataSchema).getValue();
  }
}
