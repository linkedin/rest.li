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

/* $Id$ */
package com.linkedin.r2.filter;

/**
 * A marker interface for filters. This filter is the most generic and does not provide any
 * filtering capability. See {@link com.linkedin.r2.filter.message.MessageFilter},
 * {@link com.linkedin.r2.filter.message.rpc.RpcFilter}, {@link com.linkedin.r2.filter.message.rest.RestFilter},
 * etc., for interfaces that do provide filtering capabilities.
 *
 * @author Chris Pettitt
 * @version $Revision$
 * @see com.linkedin.r2.filter.message.MessageFilter
 * @see com.linkedin.r2.filter.message.rpc.RpcFilter
 * @see com.linkedin.r2.filter.message.rest.RestFilter
 */
public interface Filter
{

}
