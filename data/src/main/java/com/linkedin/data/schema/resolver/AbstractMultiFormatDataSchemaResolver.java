/*
 * Copyright 2015 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.data.schema.resolver;

import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.grammar.PdlSchemaParserFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Combines multiple file format specific resolvers (and respective file format specific parsers)
 * into a single resolver.
 *
 * Concrete implementations should initialize the list of resolvers to use by calling
 * {@link #addResolver(DataSchemaResolver)}
 *
 * E.g. a resolver for the ".pdsc" file format and the ".pdl" file format, each with their
 * own file format specific parsers, can be combined into a single resolver able to look up
 * schemas of either file format.
 */
public abstract class AbstractMultiFormatDataSchemaResolver implements DataSchemaResolver
{
  /**
   * File extensions for all builtin parsers: PDSC, PDL.
   */
  public static final String[] BUILTIN_EXTENSIONS = new String[] {SchemaParser.FILE_EXTENSION, PdlSchemaParser.FILE_EXTENSION
  };

  private final List<DataSchemaResolver> _resolvers = new ArrayList<>();
  private List<SchemaDirectory> _schemaDirectories = Collections.singletonList(SchemaDirectoryName.PEGASUS);

  public static List<DataSchemaParserFactory> BUILTIN_FORMAT_PARSER_FACTORIES;
  static {
    BUILTIN_FORMAT_PARSER_FACTORIES = new ArrayList<>(2);
    BUILTIN_FORMAT_PARSER_FACTORIES.add(PdlSchemaParserFactory.instance());
    BUILTIN_FORMAT_PARSER_FACTORIES.add(SchemaParserFactory.instance());
  }

  /**
   * Add a resolver to the list of supported resolvers
   *
   * @param resolver Resolver that supports one format of pegasus schema.
   */
  protected void addResolver(DataSchemaResolver resolver)
  {
    this._resolvers.add(resolver);
  }

  @Override
  public Map<String, NamedDataSchema> bindings()
  {
    Map<String, NamedDataSchema> results = new HashMap<>();
    for (DataSchemaResolver resolver: _resolvers)
    {
      results.putAll(resolver.bindings());
    }
    return results;
  }

  @Override
  public Map<String, DataSchemaLocation> nameToDataSchemaLocations()
  {
    Map<String, DataSchemaLocation> results = new HashMap<>();
    for (DataSchemaResolver resolver: _resolvers)
    {
      results.putAll(resolver.nameToDataSchemaLocations());
    }
    return results;
  }

  @Override
  public NamedDataSchema findDataSchema(String name, StringBuilder errorMessageBuilder)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      NamedDataSchema result = resolver.findDataSchema(name, errorMessageBuilder);
      if (result != null)
      {
        return result;
      }
    }
    return null;
  }

  @Override
  public void bindNameToSchema(Name name, NamedDataSchema schema, DataSchemaLocation location)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      resolver.bindNameToSchema(name, schema, location);
    }

  }

  @Override
  public NamedDataSchema existingDataSchema(String name)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      NamedDataSchema result = resolver.existingDataSchema(name);
      if (result != null)
      {
        return result;
      }
    }
    return null;
  }

  @Override
  public DataSchemaLocation existingSchemaLocation(String name)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      DataSchemaLocation result = resolver.existingSchemaLocation(name);
      if (result != null)
      {
        return result;
      }
    }
    return null;
  }

  @Override
  public boolean locationResolved(DataSchemaLocation location)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      if (resolver.locationResolved(location))
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public void addPendingSchema(String name)
  {
    for (DataSchemaResolver resolver : _resolvers)
    {
      resolver.addPendingSchema(name);
    }
  }

  @Override
  public void updatePendingSchema(String name, Boolean isParsingInclude) {
    for (DataSchemaResolver resolver : _resolvers)
    {
      resolver.updatePendingSchema(name, isParsingInclude);
    }
  }

  @Override
  public void removePendingSchema(String name)
  {
    for (DataSchemaResolver resolver: _resolvers)
    {
      resolver.removePendingSchema(name);
    }
  }

  @Override
  public LinkedHashMap<String, Boolean> getPendingSchemas()
  {
    LinkedHashMap<String, Boolean> results = new LinkedHashMap<>();
    for (DataSchemaResolver resolver: _resolvers)
    {
      results.putAll(resolver.getPendingSchemas());
    }
    return results;
  }

  @Override
  public List<SchemaDirectory> getSchemaDirectories()
  {
    return _schemaDirectories;
  }

  public void setSchemaDirectories(List<SchemaDirectory> schemaDirectories)
  {
    _schemaDirectories = schemaDirectories;
  }
}
