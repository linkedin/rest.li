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

package com.linkedin.data.schema;


import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.codec.JacksonDataCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Common base class for parsers that parse Schema objects.
 *
 * @author slim
 * @author jbetz
 */
abstract public class AbstractSchemaParser extends AbstractDataParser
{
  /**
   * Constructor with resolver.
   *
   * @param resolver to be used to find {@link DataSchema}'s.
   */
  protected AbstractSchemaParser(DataSchemaResolver resolver)
  {
    _resolver = resolver;
  }

  /**
   * Get the {@link DataSchemaResolver}.
   *
   * @return the resolver to used to find {@link DataSchema}'s, may be null
   *         if no resolver has been provided to parser.
   */
  public DataSchemaResolver getResolver()
  {
    return _resolver;
  }

  /**
   * Return the top level {@link DataSchema}'s.
   *
   * The top level DataSchema's represent the types
   * that are not defined within other types.
   *
   * @return the list of top level {@link DataSchema}'s in the
   *         order that are defined.
   */
  public List<DataSchema> topLevelDataSchemas()
  {
    return Collections.unmodifiableList(_topLevelDataSchemas);
  }

  @Override
  public Map<Object, DataLocation> dataLocationMap()
  {
    return _dataLocationMap;
  }


  /**
   * Bind name and aliases to {@link NamedDataSchema}.
   *
   * @param name to bind.
   * @param aliasNames to bind.
   * @param schema to be bound to the name.
   * @return true if all names are bound to the specified {@link NamedDataSchema}.
   */
  protected boolean bindNameToSchema(Name name, List<Name> aliasNames, NamedDataSchema schema)
  {
    boolean ok = true;
    ok &= bindNameToSchema(name, schema);
    if (aliasNames != null)
    {
      for (Name aliasName : aliasNames)
      {
        ok &= bindNameToSchema(aliasName, schema);
      }
    }
    return ok;
  }

  /**
   * Bind a name to {@link NamedDataSchema}.
   *
   * @param name to bind.
   * @param schema to be bound to the name.
   * @return true if name is bound to the specified {@link NamedDataSchema}.
   */
  public boolean bindNameToSchema(Name name, NamedDataSchema schema)
  {
    boolean ok = true;
    String fullName = name.getFullName();
    if (name.isEmpty())
    {
      ok = false;
    }
    if (ok && DataSchemaUtil.typeStringToPrimitiveDataSchema(fullName) != null)
    {
      startErrorMessage(name).append("\"").append(fullName).append("\" is a pre-defined type and cannot be redefined.\n");
      ok = false;
    }
    if (ok)
    {
      DataSchema found = getResolver().existingDataSchema(name.getFullName());
      if (found != null)
      {
        startErrorMessage(name).append("\"").append(name.getFullName()).append("\" already defined as " + found + ".\n");
        ok = false;
      }
      else
      {
        getResolver().bindNameToSchema(name, schema, getLocation());
      }
    }
    return ok;
  }

  protected void addTopLevelSchema(DataSchema schema) {
    _topLevelDataSchemas.add(schema);
  }

  private final Map<Object, DataLocation> _dataLocationMap = new IdentityHashMap<Object, DataLocation>();
  private final List<DataSchema> _topLevelDataSchemas = new ArrayList<DataSchema>();
  private final DataSchemaResolver _resolver;

}
