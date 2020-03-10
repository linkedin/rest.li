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

package com.linkedin.data.schema.resolver;


import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;


/**
 * A extremely lazy {@link com.linkedin.data.schema.DataSchemaResolver} that does not
 * resolve a name to a {@link com.linkedin.data.schema.NamedDataSchema}
 * unless the name is explicitly bound earlier.
 */
public class DefaultDataSchemaResolver extends AbstractDataSchemaResolver
{
  public DefaultDataSchemaResolver()
  {
    super(null);
  }

  public DefaultDataSchemaResolver(DataSchemaParserFactory parserFactory)
  {
    super(parserFactory);
  }

  public DefaultDataSchemaResolver(DataSchemaParserFactory parserFactory, DataSchemaResolver schemaResolver)
  {
    super(parserFactory, schemaResolver);
  }

  private static final Iterator<DataSchemaLocation> _it = Collections.<DataSchemaLocation>emptyList().iterator();

  @Override
  protected Iterator<DataSchemaLocation> possibleLocations(String name)
  {
    return _it;
  }

  @Override
  protected InputStream locationToInputStream(DataSchemaLocation location, StringBuilder errorMessageBuilder)
  {
    throw new IllegalStateException("this method should never be called");
  }
}
