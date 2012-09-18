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

/**
 * $Id: $
 */

package com.linkedin.restli.server.resources;

import com.linkedin.data.template.RecordTemplate;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * KeyValueResource is a marker interface for rest.li resources which expose a map-like interface,
 * i.e., collection or association resources.  This marker interface is used by the rest.li framework
 * to determine the key and value types for the resource.
 *
 * Note that many resources will indirectly implement this interface via {@link CollectionResource}
 * or {@link AssociationResource}.
 *
 * @param <K> - the key type of the resource
 * @param <V> - the value type of the resource
 */
public interface KeyValueResource<K, V extends RecordTemplate>
{

}
