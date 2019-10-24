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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DataSchemaConstants
{
  public static final String ALIAS_KEY = "alias";
  public static final String ALIASES_KEY = "aliases";
  public static final String DEFAULT_KEY = "default";
  public static final String DEPRECATED_KEY = "deprecated";
  public static final String DEPRECATED_SYMBOLS_KEY = "deprecatedSymbols";
  public static final String DOC_KEY = "doc";
  public static final String FIELDS_KEY = "fields";
  public static final String INCLUDE_KEY = "include";
  public static final String ITEMS_KEY = "items";
  public static final String NAME_KEY = "name";
  public static final String NAMESPACE_KEY = "namespace";
  public static final String OPTIONAL_KEY = "optional";
  public static final String ORDER_KEY = "order";
  public static final String PACKAGE_KEY = "package";
  public static final String REF_KEY = "ref";
  public static final String SIZE_KEY = "size";
  public static final String SYMBOL_DOCS_KEY = "symbolDocs";
  public static final String SYMBOL_PROPERTIES_KEY = "symbolProperties";
  public static final String SYMBOLS_KEY = "symbols";
  public static final String TYPE_KEY = "type";
  public static final String VALUES_KEY = "values";

  public static final String NULL_TYPE = "null";
  public static final String BOOLEAN_TYPE = "boolean";
  public static final String INTEGER_TYPE = "int";
  public static final String LONG_TYPE = "long";
  public static final String FLOAT_TYPE = "float";
  public static final String DOUBLE_TYPE = "double";
  public static final String BYTES_TYPE = "bytes";
  public static final String STRING_TYPE = "string";

  public static final String ARRAY_TYPE = "array";
  public static final String ENUM_TYPE = "enum";
  public static final String ERROR_TYPE = "error";
  public static final String FIXED_TYPE = "fixed";
  public static final String MAP_TYPE = "map";
  public static final String RECORD_TYPE = "record";

  public static final String TYPEREF_TYPE = "typeref";

  public static final String DISCRIMINATOR_FIELD = "fieldDiscriminator";

  public static final NullDataSchema NULL_DATA_SCHEMA = new NullDataSchema();
  public static final BooleanDataSchema BOOLEAN_DATA_SCHEMA = new BooleanDataSchema();
  public static final IntegerDataSchema INTEGER_DATA_SCHEMA = new IntegerDataSchema();
  public static final LongDataSchema LONG_DATA_SCHEMA = new LongDataSchema();
  public static final FloatDataSchema FLOAT_DATA_SCHEMA = new FloatDataSchema();
  public static final DoubleDataSchema DOUBLE_DATA_SCHEMA = new DoubleDataSchema();
  public static final BytesDataSchema BYTES_DATA_SCHEMA = new BytesDataSchema();
  public static final StringDataSchema STRING_DATA_SCHEMA = new StringDataSchema();

  public static final Set<DataSchema.Type> NAMED_DATA_SCHEMA_TYPE_SET;
  public static final Set<String> SCHEMA_KEYS;
  public static final Set<String> FIELD_KEYS;
  public static final Set<String> MEMBER_KEYS;
  public static final Set<String> RESTRICTED_UNION_ALIASES;

  public static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_]*(\\.[A-Za-z_][0-9A-Za-z_]*)*");
  public static final Pattern NAMESPACE_PATTERN = Pattern.compile("([A-Za-z_][0-9A-Za-z_]*(\\.[A-Za-z_][0-9A-Za-z_]*)*)?");
  public static final Pattern PACKAGE_PATTERN = Pattern.compile("([A-Za-z_][0-9A-Za-z_]*(\\.[A-Za-z_][0-9A-Za-z_]*)*)?");
  public static final Pattern UNQUALIFIED_NAME_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_]*");
  public static final Pattern ENUM_SYMBOL_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_]*");
  public static final Pattern FIELD_NAME_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_]*");

  static
  {
    Set<DataSchema.Type> namedSet = EnumSet.of(DataSchema.Type.ENUM,
                                               DataSchema.Type.FIXED,
                                               DataSchema.Type.RECORD,
                                               DataSchema.Type.TYPEREF);
    NAMED_DATA_SCHEMA_TYPE_SET = Collections.unmodifiableSet(namedSet);

    Set<String> schemaKeys = new HashSet<String>(Arrays.asList(ALIASES_KEY, DOC_KEY, FIELDS_KEY, INCLUDE_KEY, ITEMS_KEY, NAME_KEY,
                                                               NAMESPACE_KEY, PACKAGE_KEY, REF_KEY,
                                                               SIZE_KEY, SYMBOLS_KEY, SYMBOL_DOCS_KEY, TYPE_KEY, VALUES_KEY));
    SCHEMA_KEYS = Collections.unmodifiableSet(schemaKeys);

    Set<String> fieldKeys = new HashSet<String>(Arrays.asList(ALIASES_KEY, DEFAULT_KEY, DOC_KEY, NAME_KEY, OPTIONAL_KEY, ORDER_KEY, TYPE_KEY));
    FIELD_KEYS = Collections.unmodifiableSet(fieldKeys);

    Set<String> memberKeys = new HashSet<String>(Arrays.asList(DOC_KEY, ALIAS_KEY, TYPE_KEY));
    MEMBER_KEYS = Collections.unmodifiableSet(memberKeys);

    Set<String> restrictedUnionAliases = new HashSet<>(Arrays.asList(DISCRIMINATOR_FIELD));
    RESTRICTED_UNION_ALIASES = Collections.unmodifiableSet(restrictedUnionAliases);
  }

  private DataSchemaConstants()
  {
  }
}
