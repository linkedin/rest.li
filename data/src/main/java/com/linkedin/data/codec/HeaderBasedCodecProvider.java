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

package com.linkedin.data.codec;

import com.linkedin.data.codec.entitystream.StreamDataCodec;
import java.util.Map;


/**
 * @deprecated This interface should not be used. It only exists for backward compatibility reasons.
 */
@Deprecated
public interface HeaderBasedCodecProvider
{

  /**
   * @deprecated This method should not be invoked.
   */
  @Deprecated
  default DataCodec getCodec(Map<String, String> requestHeaders)
  {
    throw new IllegalStateException("This method should not be invoked.");
  }

  /**
   * @deprecated This method should not be invoked.
   */
  @Deprecated
  default StreamDataCodec getStreamCodec(Map<String, String> requestHeaders)
  {
    throw new IllegalStateException("This method should not be invoked.");
  }
}