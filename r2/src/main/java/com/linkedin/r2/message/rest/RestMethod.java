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
package com.linkedin.r2.message.rest;

/**
 * Old-style enum to list commonly used REST methods.<p/>
 *
 * We don't use a {@link Enum}, which is in most cases more convenient, because we preserve any
 * method we receive - even when we don't know how to map it to a symbolic name.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestMethod
{
  public static final String DELETE = "DELETE";
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";

  private RestMethod() {}
}
