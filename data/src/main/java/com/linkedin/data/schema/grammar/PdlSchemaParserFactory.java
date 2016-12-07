/*
 Copyright 2015 Coursera Inc.

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

package com.linkedin.data.schema.grammar;

import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;

/**
 *  DataSchemaParserFactory for the Pegasus data language (.pdl).
 *
 *  @author Joe Betz
 */
public class PdlSchemaParserFactory implements DataSchemaParserFactory {
  private static PdlSchemaParserFactory INSTANCE = new PdlSchemaParserFactory();

  public PdlSchemaParserFactory() {}

  public AbstractSchemaParser create(DataSchemaResolver resolver)
  {
    return new PdlSchemaParser(resolver);
  }

  public String getLanguageExtension()
  {
    return "pdl";
  }

  static public PdlSchemaParserFactory instance()
  {
    return INSTANCE;
  }
}
