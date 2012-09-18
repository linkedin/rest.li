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

package com.linkedin.data.avro;

public interface DataTranslatorContext
{
  /**
   * Create a new {@link com.linkedin.data.message.Message} with the given format and args,
   * and append it to the list of {@link com.linkedin.data.message.Message}s.
   *
   * @param format provides the format string, see {@link java.util.Formatter}.
   * @param args provides the arguments to be formatted using the provided format string.
   */
  void appendMessage(String format, Object... args);
}
