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
package com.linkedin.r2.message.stream;

import com.linkedin.r2.message.Request;

/**
 * An object that contains details of a REST stream request.
 * StreamRequest contains an EntityStream as its entity, which can only be consumed once.
 *
 * @author Zhenkai Zhu
 */
public interface StreamRequest extends Request, StreamMessage
{
  @Override
  StreamRequestBuilder builder();
}
