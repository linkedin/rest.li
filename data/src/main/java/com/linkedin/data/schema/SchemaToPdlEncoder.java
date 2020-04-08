/*
   Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;


/**
 * Encodes {@link DataSchema} types to Pegasus data language (.pdl) source code.
 */
public class SchemaToPdlEncoder extends AbstractSchemaEncoder
{
  // Unions with at least this many members will be written onto multiple lines to improve readability
  private static final int UNION_MULTILINE_THRESHOLD = 5;

  /**
   * Encode a {@link DataSchema} to a PDL encoded string.
   *
   * @param schema is the {@link DataSchema} to build a PDL encoded output for.
   * @param encodingStyle is the encoding style.
   * @return the PDL encoded string representing the {@link DataSchema}.
   */
  public static String schemaToPdl(DataSchema schema, EncodingStyle encodingStyle)
  {
    StringWriter writer = new StringWriter();

    SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
    encoder.setEncodingStyle(encodingStyle);

    try
    {
      encoder.encode(schema);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }

    return writer.toString();
  }

  /**
   * Encoding style for PDL.
   */
  public enum EncodingStyle
  {
    /**
     * As compact as possible.
     */
    COMPACT(new CompactPdlBuilder.Provider()),

    /**
     * Very neat and human-readable, using newlines and indentation.
     */
    INDENTED(new IndentedPdlBuilder.Provider());

    // Provider for creating new PDL builder instances which can encode in this style
    PdlBuilder.Provider _pdlBuilderProvider;

    EncodingStyle(PdlBuilder.Provider pdlBuilderProvider)
    {
      _pdlBuilderProvider = pdlBuilderProvider;
    }

    PdlBuilder newBuilderInstance(Writer writer)
    {
      return _pdlBuilderProvider.newInstance(writer);
    }
  }

  private final Writer _writer;

  // Configurable options
  private EncodingStyle _encodingStyle;

  // Stateful variables used on a per-encoding basis
  private PdlBuilder _builder;
  private Map<String, Name> _importsByLocalName;
  private String _namespace = "";
  private String _package = "";

  /**
   * Construct a .pdl source code encoder.
   *
   * @param out provides the encoded .pdl destination.
   */
  public SchemaToPdlEncoder(Writer out)
  {
    _writer = out;
    _encodingStyle = EncodingStyle.INDENTED;
  }

  /**
   * Set the preferred {@link EncodingStyle}.
   *
   * @param encodingStyle preferred encoding style
   */
  public void setEncodingStyle(EncodingStyle encodingStyle)
  {
    _encodingStyle = encodingStyle;
  }

  /**
   * Write the provided schema as the top level type in a .pdl file.
   *
   * @param schema provides the schema to encode to .pdl and emit to this instance's writer.
   * @throws IOException if a writer IO exception occurs.
   */
  @Override
  public void encode(DataSchema schema) throws IOException
  {
    // Initialize a new builder for the preferred encoding style
    _builder = _encodingStyle.newBuilderInstance(_writer);

    // Set and write root namespace/package
    if (schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedSchema = (NamedDataSchema) schema;
      boolean hasNamespace = StringUtils.isNotBlank(namedSchema.getNamespace());
      boolean hasPackage = StringUtils.isNotBlank(namedSchema.getPackage());
      if (hasNamespace || hasPackage)
      {
        if (hasNamespace)
        {
          _builder.write("namespace")
              .writeSpace()
              .writeIdentifier(namedSchema.getNamespace())
              .newline();
          _namespace = namedSchema.getNamespace();
        }
        if (hasPackage)
        {
          _builder.write("package")
              .writeSpace()
              .writeIdentifier(namedSchema.getPackage())
              .newline();
          _package = namedSchema.getPackage();
        }
        _builder.newline();
      }
    }

    // Compute imports
    if (_typeReferenceFormat != TypeReferenceFormat.DENORMALIZE)
    {
      _importsByLocalName = computeImports(schema, _namespace);
    }
    else
    {
      _importsByLocalName = Collections.emptyMap();
    }

    // Write imports sorted by fully qualified name
    if (_importsByLocalName.size() > 0)
    {
      for (Name importName : new TreeSet<>(_importsByLocalName.values()))
      {
        _builder.write("import")
            .writeSpace()
            .writeIdentifier(importName.getFullName())
            .newline();
      }
      _builder.newline();
    }

    // Write the schema
    writeInlineSchema(schema);
  }

  /**
   * Write a schema as inline code, not including any namespace, package or import preamble.
   *
   * @param schema provides the schema to write.
   */
  private void writeInlineSchema(DataSchema schema) throws IOException
  {
    // Begin overridden namespace scope, if any
    boolean hasNamespaceOverride = false;
    boolean hasPackageOverride = false;
    final String surroundingNamespace = _namespace;
    final String surroundingPackage = _package;
    if (schema instanceof NamedDataSchema) {
      markEncountered(schema);
      NamedDataSchema namedSchema = (NamedDataSchema) schema;
      hasNamespaceOverride = !StringUtils.isEmpty(namedSchema.getNamespace()) && !namedSchema.getNamespace().equals(surroundingNamespace);
      hasPackageOverride = !StringUtils.isEmpty(namedSchema.getPackage()) && !namedSchema.getPackage().equals(surroundingPackage);
      if (hasNamespaceOverride || hasPackageOverride)
      {
        _builder.write("{")
            .newline()
            .increaseIndent()
            .indent();
        if (hasNamespaceOverride)
        {
          _builder.write("namespace")
              .writeSpace()
              .writeIdentifier(namedSchema.getNamespace())
              .newline()
              .indent();
          _namespace = namedSchema.getNamespace();
        }
        if (hasPackageOverride)
        {
          _builder.write("package")
              .writeSpace()
              .writeIdentifier(namedSchema.getPackage())
              .newline()
              .indent();
          _package = namedSchema.getPackage();
        }
      }
    }

    // Write the inlined schema
    switch (schema.getType())
    {
      case RECORD:
        writeRecord((RecordDataSchema) schema);
        break;
      case ENUM:
        writeEnum((EnumDataSchema) schema);
        break;
      case FIXED:
        writeFixed((FixedDataSchema) schema);
        break;
      case TYPEREF:
        writeTyperef((TyperefDataSchema) schema);
        break;
      case ARRAY:
        writeArray((ArrayDataSchema) schema);
        break;
      case MAP:
        writeMap((MapDataSchema) schema);
        break;
      case UNION:
        writeUnion((UnionDataSchema) schema);
        break;
      case BOOLEAN:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case STRING:
      case BYTES:
        writePrimitive((PrimitiveDataSchema) schema);
        break;
      case NULL:
        _builder.write("null");
        break;
      default:
        throw new IllegalArgumentException("Unrecognized schema type " + schema.getClass());
    }

    // End overridden namespace scope
    if (hasNamespaceOverride || hasPackageOverride) {
      _builder.decreaseIndent()
          .newline()
          .indent()
          .write("}");
      _namespace = surroundingNamespace;
      _package = surroundingPackage;
    }
  }

  private void writeRecord(RecordDataSchema schema) throws IOException
  {
    writeDocAndProperties(schema);
    _builder.write("record")
        .writeSpace()
        .writeIdentifier(schema.getName());

    List<NamedDataSchema> includes = schema.getInclude();
    if (includes.size() > 0 && !schema.isFieldsBeforeIncludes())
    {
      writeIncludes(schema, includes);
    }
    _builder.writeSpace().write("{");

    // This check allows for field-less records to be wholly inlined (e.g. "record A {}")
    List<RecordDataSchema.Field> fields = schema.getFields();
    if (!fields.isEmpty())
    {
      _builder.newline().increaseIndent();

      for (RecordDataSchema.Field field : fields)
      {
        if (field.getRecord().equals(schema))
        {
          writeField(field);
        }
      }
      _builder.decreaseIndent().indent();
    }

    _builder.write("}");

    if (includes.size() > 0 && schema.isFieldsBeforeIncludes())
    {
      writeIncludes(schema, includes);
    }
  }

  /**
   * Writes a {@link com.linkedin.data.schema.RecordDataSchema.Field} to .pdl.
   * @param field record field
   */
  private void writeField(RecordDataSchema.Field field) throws IOException
  {
    writeDocAndProperties(field);
    _builder.indent()
        .writeIdentifier(field.getName())
        .write(":")
        .writeSpace();
    if (field.getOptional())
    {
      _builder.write("optional").writeSpace();
    }
    writeReferenceOrInline(field.getType(), field.isDeclaredInline());

    if (field.getDefault() != null)
    {
      _builder.writeSpace()
          .write("=")
          .writeSpace()
          .writeJson(field.getDefault());
    }
    _builder.newline();
  }

  private void writeIncludes(RecordDataSchema schema, List<NamedDataSchema> includes) throws IOException {
    _builder.writeSpace()
        .write("includes")
        .writeSpace();
    for (Iterator<NamedDataSchema> iter = includes.iterator(); iter.hasNext();)
    {
      NamedDataSchema include = iter.next();
      writeReferenceOrInline(include, schema.isIncludeDeclaredInline(include));
      if (iter.hasNext())
      {
        _builder.writeComma().writeSpace();
      }
    }
  }

  private void writeEnum(EnumDataSchema schema) throws IOException
  {
    // Retrieve symbol properties and deprecated symbols from the properties map
    Map<String, Object> properties = schema.getProperties();
    DataMap propertiesMap = new DataMap(coercePropertyToDataMapOrFail(schema,
        DataSchemaConstants.SYMBOL_PROPERTIES_KEY,
        properties.get(DataSchemaConstants.SYMBOL_PROPERTIES_KEY)));
    DataMap deprecatedMap = coercePropertyToDataMapOrFail(schema,
        DataSchemaConstants.DEPRECATED_SYMBOLS_KEY,
        properties.get(DataSchemaConstants.DEPRECATED_SYMBOLS_KEY));

    writeDocAndProperties(schema);
    _builder.write("enum")
        .writeSpace()
        .writeIdentifier(schema.getName())
        .writeSpace()
        .write("{")
        .newline()
        .increaseIndent();

    Map<String, String> docs = schema.getSymbolDocs();

    for (String symbol : schema.getSymbols())
    {
      String docString = docs.get(symbol);
      DataMap symbolProperties = coercePropertyToDataMapOrFail(schema,
          DataSchemaConstants.SYMBOL_PROPERTIES_KEY + "." + symbol,
          propertiesMap.get(symbol));
      Object deprecated = deprecatedMap.get(symbol);
      if (deprecated != null)
      {
        symbolProperties.put("deprecated", deprecated);
      }

      if (StringUtils.isNotBlank(docString) || !symbolProperties.isEmpty())
      {
        // For any non-trivial symbol declarations, separate with an additional newline.
        _builder.newline();
      }
      writeDocAndProperties(docString, symbolProperties);
      _builder.indent()
          .writeIdentifier(symbol)
          .newline();
    }
    _builder.decreaseIndent()
        .indent()
        .write("}");
  }

  private void writeFixed(FixedDataSchema schema) throws IOException
  {
    writeDocAndProperties(schema);
    _builder.write("fixed")
        .writeSpace()
        .writeIdentifier(schema.getName())
        .writeSpace()
        .write(String.valueOf(schema.getSize()));
  }

  private void writeTyperef(TyperefDataSchema schema) throws IOException
  {
    writeDocAndProperties(schema);
    _builder.write("typeref")
        .writeSpace()
        .writeIdentifier(schema.getName())
        .writeSpace()
        .write("=")
        .writeSpace();
    DataSchema ref = schema.getRef();
    writeReferenceOrInline(ref, schema.isRefDeclaredInline());
  }

  private void writeMap(MapDataSchema schema) throws IOException
  {
    writeProperties(schema.getProperties());
    _builder.write("map[string")
        .writeComma()
        .writeSpace();
    writeReferenceOrInline(schema.getValues(), schema.isValuesDeclaredInline());
    _builder.write("]");
  }

  private void writeArray(ArrayDataSchema schema) throws IOException
  {
    writeProperties(schema.getProperties());
    _builder.write("array[");
    writeReferenceOrInline(schema.getItems(), schema.isItemsDeclaredInline());
    _builder.write("]");
  }

  /**
   * Writes a union data schema to .pdl.
   * @param schema union data schema
   */
  private void writeUnion(UnionDataSchema schema) throws IOException
  {
    writeProperties(schema.getProperties());
    _builder.write("union[");
    final boolean useMultilineFormat = schema.areMembersAliased() || schema.getMembers().size() >= UNION_MULTILINE_THRESHOLD;
    if (useMultilineFormat)
    {
      _builder.newline().increaseIndent();
    }
    for (Iterator<UnionDataSchema.Member> iter = schema.getMembers().iterator(); iter.hasNext();)
    {
      writeUnionMember(iter.next(), useMultilineFormat);
      if (iter.hasNext())
      {
        if (useMultilineFormat)
        {
          _builder.writeComma().newline();
        }
        else
        {
          _builder.writeComma().writeSpace();
        }
      }
    }
    if (useMultilineFormat)
    {
      _builder.decreaseIndent()
          .newline()
          .indent();
    }
    _builder.write("]");
  }

  /**
   * Writes a union member to .pdl.
   * @param member union data schema member
   * @param useMultilineFormat whether the union containing this member is being written onto multiple lines
   */
  private void writeUnionMember(UnionDataSchema.Member member, boolean useMultilineFormat) throws IOException
  {
    if (member.hasAlias())
    {
      if (StringUtils.isNotBlank(member.getDoc()) || !member.getProperties().isEmpty() || member.isDeclaredInline())
      {
        // For any non-trivial union member declarations, separate with an additional newline.
        _builder.newline();
      }
      writeDocAndProperties(member.getDoc(), member.getProperties());
      _builder.indent()
          .writeIdentifier(member.getAlias())
          .write(":")
          .writeSpace();
    }
    else if (useMultilineFormat)
    {
      // Necessary because "null" union members aren't aliased
      _builder.indent();
    }
    writeReferenceOrInline(member.getType(), member.isDeclaredInline());
  }

  private void writePrimitive(PrimitiveDataSchema schema) throws IOException
  {
    _builder.write(schema.getUnionMemberKey());
  }

  /**
   * Coerces a schema property value to a DataMap or, if it cannot be coerced, throws an exception.
   * If the value is a DataMap, return it.  If the value is null, return an empty DataMap.
   * @param schema provides the schema this property belongs to, for error reporting purposes.
   * @param name provides the schema's property path to this value as a string, for error reporting purposes.
   * @param value provides the property value to coerce.
   * @return the property value, coerced to a DataMap.
   * @throws IllegalArgumentException if the property value cannot be coerced to a DataMap.
   */
  private DataMap coercePropertyToDataMapOrFail(NamedDataSchema schema, String name, Object value)
  {
    if (value == null)
    {
      return new DataMap();
    }
    if (!(value instanceof DataMap))
    {
      throw new IllegalArgumentException("'" + name + "' in " + schema.getFullName() +
          " must be of type DataMap, but is: " + value.getClass());
    }
    return (DataMap) value;
  }



  /**
   * Writes a data schema type to .pdl code, either as a by-name reference, or as an inlined declaration.
   *
   * This instance's TypeReferenceFormat is respected. If DENORMALIZE, the schema is inlined at it's first lexical
   * appearance. If PRESERVE, it is inlined only if it was originally inlined.
   *
   * @param dataSchema provides the data schema to write.
   * @param originallyInlined if true, the original schema inlined this type declaration, otherwise it used a by-name
   *                         reference.
   */
  private void writeReferenceOrInline(DataSchema dataSchema, boolean originallyInlined) throws IOException
  {
    TypeRepresentation representation = selectTypeRepresentation(dataSchema, originallyInlined);
    encodeNamedInnerSchema(dataSchema, representation);
  }

  /**
   * Writes a data schema type to .pdl code, either as a by-name reference, or as an inlined declaration.
   *
   *
   * @param dataSchema provides the data schema to write.
   * @param representation if it is declared_inline, the original schema inlined this type declaration, otherwise it is a by-name
   *                         reference.
   * @throws IllegalArgumentException if the typeRepresentation is by-name reference and dataSchema type is not NamedDataSchema.
   */
  protected void encodeNamedInnerSchema(DataSchema dataSchema, TypeRepresentation representation) throws IOException
  {
    if (representation == TypeRepresentation.DECLARED_INLINE)
    {
      boolean requiresNewlineLayout = requiresNewlineLayout(dataSchema);
      if (requiresNewlineLayout) {
        _builder.newline().increaseIndent();
      }
      writeInlineSchema(dataSchema);
      if (requiresNewlineLayout) {
        _builder.decreaseIndent();
      }
    }
    else
    {
      if (dataSchema instanceof NamedDataSchema)
      {
        markEncountered(dataSchema);
        writeReference((NamedDataSchema) dataSchema);
      }
      else
      {
        throw new IllegalArgumentException("Unnamed not marked as inline: " + dataSchema);
      }
    }
  }

  /**
   * For inline declarations, determine if a type requires a newline to be declared. Only types without a
   * doc string, properties, or aliases can initiate their declaration as a continuation of an existing line
   * (e.g. "fieldName: record Example {}").
   *
   * @param dataSchema provides the type to check for layout requirements.
   * @return true if the type requires a newline for layout
   */
  private boolean requiresNewlineLayout(DataSchema dataSchema)
  {
    if (dataSchema instanceof NamedDataSchema)
    {
      NamedDataSchema named = (NamedDataSchema) dataSchema;
      return StringUtils.isNotBlank(named.getDoc()) || !named.getProperties().isEmpty() || !named.getAliases().isEmpty();
    }
    else if (dataSchema instanceof ComplexDataSchema)
    {
      return !dataSchema.getProperties().isEmpty();
    }
    return false;
  }

  /**
   * Writes a set of schema properties to .pdl.
   * @param properties provides the properties to write.
   */
  private boolean writeProperties(Map<String, Object> properties) throws IOException
  {
    _builder.writeProperties(Collections.emptyList(), properties);
    return !properties.isEmpty();
  }

  /**
   * Write a doc string and a set of properties to this encoder's writer.
   * @param doc doc string
   * @param properties mapping of property paths to property values
   * @return whether any doc string or properties were written at all
   */
  private boolean writeDocAndProperties(String doc, Map<String, Object> properties) throws IOException
  {
    final boolean hasDoc = _builder.writeDoc(doc);
    final boolean hasProperties = writeProperties(properties);
    return hasDoc || hasProperties;
  }

  /**
   * Write the doc string and properties for a {@link NamedDataSchema}, including attributes that aren't really
   * properties but are written as such (e.g. aliases).
   * @param schema named data schema
   */
  private void writeDocAndProperties(NamedDataSchema schema) throws IOException
  {
    // Add all schema properties
    final DataMap properties = new DataMap(schema.getProperties());

    // Remove enum reserved keys
    if (schema instanceof EnumDataSchema)
    {
      properties.remove(DataSchemaConstants.DEPRECATED_SYMBOLS_KEY);
      properties.remove(DataSchemaConstants.SYMBOL_PROPERTIES_KEY);
    }

    // Add aliases
    final List<Name> aliases = schema.getAliases();
    if (aliases != null && aliases.size() > 0)
    {
      List<String> aliasStrings = aliases.stream()
          .map(Name::getFullName)
          .collect(Collectors.toList());
      properties.put(DataSchemaConstants.ALIASES_KEY, new DataList(aliasStrings));
    }

    final boolean hasDocOrProperties = writeDocAndProperties(schema.getDoc(), properties);

    // If anything was written, indentation needs to be corrected
    if (hasDocOrProperties) {
      _builder.indent();
    }
  }

  /**
   * Write the doc string and properties for a {@link com.linkedin.data.schema.RecordDataSchema.Field}, including
   * attributes that aren't really properties but are written as such (e.g. aliases, order).
   * @param field record field
   */
  private void writeDocAndProperties(RecordDataSchema.Field field) throws IOException
  {
    final DataMap properties = new DataMap(field.getProperties());

    // Add aliases property
    final List<String> aliases = field.getAliases();
    if (aliases != null && !aliases.isEmpty())
    {
      properties.put(DataSchemaConstants.ALIASES_KEY, new DataList(aliases));
    }

    // Add order property
    if (field.getOrder() != null && !field.getOrder().equals(RecordDataSchema.Field.Order.ASCENDING))
    {
      properties.put(DataSchemaConstants.ORDER_KEY, field.getOrder().name());
    }

    // For any non-trivial field declarations, separate with an additional newline
    if (StringUtils.isNotBlank(field.getDoc()) || !properties.isEmpty() || field.isDeclaredInline())
    {
      _builder.newline();
    }

    writeDocAndProperties(field.getDoc(), properties);
  }

  /**
   * Calculates which types to import to minimize the need to fully qualify names in a .pdl source file. The resulting
   * import list includes only types that should be explicitly written as import statements in the .pdl source.
   *
   * The following rules are used to determine whether a type should be imported:
   * (1) The type is outside the root namespace of the document.
   * (2) The type is declared outside the document (i.e. not inlined in this document).
   * (3) The type's name does not conflict with name of an Inlined type.
   * (4) Importing the type should not force using FQN for another type that is in the same namespace as its
   *     surrounding.
   *
   * When multiple referenced types with the same unqualified name may be imported, the type with the alphabetically
   * first namespace is chosen. (e.g. "com.a.b.c.Foo" is chosen over "com.x.y.z.Foo")
   *
   * Any type that is not imported and is not within the namespace from which it's referenced must be referenced by
   * fully qualified name through the .pdl source.
   *
   * @param schema provide the top level schema to calculate imports for.
   * @param rootNamespace the root namespace of this document.
   * @return a sorted map of schema type names to import, keyed by local name.
   */
  private Map<String, Name> computeImports(DataSchema schema, String rootNamespace)
  {
    Set<Name> encounteredTypes = new HashSet<>();
    // Collects the set of simple names of types that can cause conflicts with imports because
    // 1. They are defined inline or
    // 2. They are in the same namespace as their surrounding context (including namespace overrides) and are
    //    preferred use simple reference
    Set<String> nonImportableTypeNames = new HashSet<>();
    gatherTypes(schema, true, encounteredTypes, nonImportableTypeNames, rootNamespace);

    // Filter out types that shouldn't have an import and return as a mapping from simple name to typed name
    return encounteredTypes
        .stream()
        .filter(name -> !name.getNamespace().equals(rootNamespace)
            && !nonImportableTypeNames.contains(name.getName()))
        .collect(Collectors.toMap(
            Name::getName,
            Function.identity(),
            // Resolve name conflicts alphabetically
            (Name nameA, Name nameB) -> nameA.compareTo(nameB) < 0 ? nameA : nameB
        ));
  }

  /**
   * Gather all types (both referenced and inlined) and names of types that should use simple reference from this schema
   * and in all its descendents.
   * @param schema schema to traverse.
   * @param isDeclaredInline true if the schema should be treated as an inline declaration, false if it should be
   *                         considered a by-name reference.
   * @param encounteredTypes cumulative set of all encountered types in this schema (and its descendents).
   * @param nonImportableTypeNames cumulative set of simple names of all types in this schema (and its descendents)
   *                              that can conflict with imports.
   * @param currentNamespace namespace of the current scope.
   */
  private void gatherTypes(DataSchema schema, boolean isDeclaredInline, Set<Name> encounteredTypes,
      Set<String> nonImportableTypeNames, String currentNamespace)
  {
    // If named type, add to the set of encountered types
    if (schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedSchema = (NamedDataSchema) schema;
      encounteredTypes.add(new Name(namedSchema.getFullName()));
      // If declared inline or of the namespace matches the current namespace, add to the set of non-importable
      // simple names.
      if (isDeclaredInline || currentNamespace.equals(namedSchema.getNamespace()))
      {
        nonImportableTypeNames.add(namedSchema.getName());
      }
    }

    // Continue recursively traversing the schema
    if (isDeclaredInline)
    {
      if (schema instanceof RecordDataSchema)
      {
        RecordDataSchema recordSchema = (RecordDataSchema) schema;
        for (RecordDataSchema.Field field : recordSchema.getFields())
        {
          // Process only fields that are part of this schema (ignore included fields).
          if (field.getRecord().equals(schema)) {
            gatherTypes(field.getType(), field.isDeclaredInline(), encounteredTypes, nonImportableTypeNames,
                recordSchema.getNamespace());
          }
        }
        for (NamedDataSchema include : recordSchema.getInclude())
        {
          gatherTypes(include, recordSchema.isIncludeDeclaredInline(include), encounteredTypes,
              nonImportableTypeNames, recordSchema.getNamespace());
        }
      }
      else if (schema instanceof TyperefDataSchema)
      {
        TyperefDataSchema typerefSchema = (TyperefDataSchema) schema;
        gatherTypes(typerefSchema.getRef(), typerefSchema.isRefDeclaredInline(), encounteredTypes,
            nonImportableTypeNames, typerefSchema.getNamespace());
      }
      else if (schema instanceof UnionDataSchema)
      {
        UnionDataSchema unionSchema = (UnionDataSchema) schema;
        for (UnionDataSchema.Member member : unionSchema.getMembers())
        {
          gatherTypes(member.getType(), member.isDeclaredInline(), encounteredTypes, nonImportableTypeNames,
              currentNamespace);
        }
      }
      else if (schema instanceof MapDataSchema)
      {
        MapDataSchema mapSchema = (MapDataSchema) schema;
        gatherTypes(mapSchema.getValues(), mapSchema.isValuesDeclaredInline(), encounteredTypes,
            nonImportableTypeNames, currentNamespace);
      }
      else if (schema instanceof ArrayDataSchema)
      {
        ArrayDataSchema arraySchema = (ArrayDataSchema) schema;
        gatherTypes(arraySchema.getItems(), arraySchema.isItemsDeclaredInline(), encounteredTypes,
            nonImportableTypeNames, currentNamespace);
      }
    }
  }

  /**
   * Writes the .pdl escaped source identifier for the given named type. Writes either the simple or fully qualified
   * name based on the imports in the document and current namespace.
   *
   * @param schema the named schema to get a .pdl escaped source identifier for.
   */
  private void writeReference(NamedDataSchema schema) throws IOException
  {
    // Imports take precedence over current namespace
    if (_importsByLocalName.containsKey(schema.getName()) &&
        _importsByLocalName.get(schema.getName()).getNamespace().equals(schema.getNamespace()))
    {
      // Write only simple name if there is an import matching the schema.
      _builder.writeIdentifier(schema.getName());
    }
    else if (_namespace.equals(schema.getNamespace()) && !_importsByLocalName.containsKey(schema.getName()))
    {
      // Write only simple name for schemas in the current namespace only if there are no conflicting imports.
      _builder.writeIdentifier(schema.getName());
    }
    else
    {
      _builder.writeIdentifier(schema.getFullName());
    }
  }
}
