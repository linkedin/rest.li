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

package com.linkedin.data.codec;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;


/**
 * {@link JacksonDataCodec} that uses Jackson DefaultPrettyPrinter.
 *
 * @author Keren Jin
 */
public class PrettyPrinterJacksonDataCodec extends JacksonDataCodec
{
  public PrettyPrinterJacksonDataCodec()
  {
    super();
    setPrettyPrinter(new DefaultPrettyPrinter());
  }

  public PrettyPrinterJacksonDataCodec(JsonFactory jsonFactory)
  {
    super(jsonFactory);
    setPrettyPrinter(new DefaultPrettyPrinter());
  }
}
