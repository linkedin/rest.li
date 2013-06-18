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


import com.linkedin.data.template.RecordTemplate;


/**
 * SingleObjectResource is a marker interface for rest.li resources which expose a single object interface,
 * i.e., simple resource.  This marker interface is used by the rest.li framework
 * to determine value types for the resource.
 *
 * Note that many resources will indirectly implement this interface via {@link SimpleResource}
 *
 * @param <V> - the value type of the resource
 */
public interface SingleObjectResource <V extends RecordTemplate>
{
}