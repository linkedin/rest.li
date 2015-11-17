/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.filter.compression.streaming;

import com.linkedin.r2.message.stream.entitystream.EntityStream;


/**
 * @author Ang Xu
 */
public interface StreamingCompressor
{
  /**
   * @return Corresponding value for the content-encoding for the implemented
   * compression method.
   * */
  String getContentEncodingName();

  /** Decompression function.
   * @param input     EntityStream to be decompressed
   * @return Newly created EntityStream of decompressed of data, or null if error
   * */
  EntityStream inflate(EntityStream input);

  /** Compress function.
   * @param input     EntityStream to be compressed
   * @return Newly created EntityStream of compressed data, or null if error
   * */
  EntityStream deflate(EntityStream input);
}
