/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * Enumerates the user events potentially raised in the {@link ChannelPipeline}.
 *
 * @author Sean Sheng
 */
public enum ChannelPipelineEvent
{
  /**
   * User event raised in the {@link ChannelPipeline} that indicates the
   * response is fully received and the {@link Channel} is ready to be
   * returned or disposed.
   */
  RESPONSE_COMPLETE
}
