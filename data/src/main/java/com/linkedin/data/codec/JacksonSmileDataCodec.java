/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;


/**
 * A codec that serializes to and deserializes from
 * <a href="https://github.com/FasterXML/smile-format-specification">Smile</a> encoded data.
 *
 * @author kramgopa
 */
public class JacksonSmileDataCodec extends AbstractJacksonDataCodec
{
  public JacksonSmileDataCodec()
  {
    this(createDefaultSmileFactory());
  }

  public JacksonSmileDataCodec(SmileFactory smileFactory)
  {
    super(smileFactory);

    // Always disable field name interning.
    smileFactory.disable(SmileFactory.Feature.INTERN_FIELD_NAMES);
  }

  private static SmileFactory createDefaultSmileFactory()
  {
    SmileFactory factory = new SmileFactory();

    // Enable field name and string value sharing by default.
    factory.enable(SmileGenerator.Feature.CHECK_SHARED_NAMES);
    factory.enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES);

    return factory;
  }
}
