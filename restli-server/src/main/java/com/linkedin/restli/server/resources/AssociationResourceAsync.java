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

package com.linkedin.restli.server.resources;

import java.util.Map;
import java.util.Set;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;



/**
 *
 * @author femekci
 */
public interface AssociationResourceAsync<V extends RecordTemplate>
        extends BaseResource, KeyValueResource<CompoundKey, V>
{
  void batchGet(Set<CompoundKey> ids, Callback<Map<CompoundKey, V>> callback);

  void get(CompoundKey key, Callback<V> callback);

  void update(CompoundKey key, V entity, Callback<UpdateResponse> callback);

  void update(CompoundKey key, PatchRequest<V> patch, Callback<UpdateResponse> callback);

  void delete(CompoundKey key, Callback<UpdateResponse> callback);

}
