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

package com.linkedin.restli.server.multiplexer;


/**
 * MultiplexerRunMode specifies if all requests belonging to the {@link MultiplexedRequest} will
 * be executed as a single ParSeq plan ({@link #SINGLE_PLAN}) or if each request that belongs to the
 * {@code MultiplexedRequest} will be executed as a separate ParSeq plan ({@link #MULTIPLE_PLANS}).
 * <p>
 * {@link #SINGLE_PLAN} allows optimizations such as batching but it means that all tasks will be
 * executed in sequence. {@link #MULTIPLE_PLANS} can potentially speed up execution because requests
 * can execute physically in parallel but some ParSeq optimization will not work across different plans.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
public enum MultiplexerRunMode
{
  SINGLE_PLAN,
  MULTIPLE_PLANS
}
