package com.linkedin.data.schema;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * Encodes {@link DataSchema} types to Pegasus data language (.pdl) source code.
 */
public class SchemaToPdlEncoder extends AbstractSchemaEncoder
{
  private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
    "array", "enum", "fixed", "import", "includes", "map", "namespace", "package",
		"record", "typeref", "union", "null", "true", "false"
  ));

  private static final JacksonDataCodec CODEC = new JacksonDataCodec();

  private final Writer _out;
  private Map<String, Name> _importsByLocalName;
  private int _indentDepth = 0;
  private String _namespace = null;

  /**
   * Construct a .pdl source code encoder.
   *
   * @param out provides the encoded .pdl destination.
   */
  public SchemaToPdlEncoder(Writer out)
  {
    _out = out;
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
    if (_typeReferenceFormat != TypeReferenceFormat.DENORMALIZE)
    {
      _importsByLocalName = computeImports(schema);
    }
    else
    {
      _importsByLocalName = Collections.emptyMap();
    }

    if (schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedSchema = (NamedDataSchema) schema;
      boolean hasNamespace = StringUtils.isNotBlank(namedSchema.getNamespace());
      boolean hasPackage = StringUtils.isNotBlank(namedSchema.getPackage());
      if (hasNamespace || hasPackage)
      {
        if (hasNamespace)
        {
          writeLine("namespace " + escapeIdentifier(namedSchema.getNamespace()));
          _namespace = namedSchema.getNamespace();
        }
        if (hasPackage)
        {
          writeLine("package " + escapeIdentifier(namedSchema.getPackage()));
        }
        newline();
      }

      if (_importsByLocalName.size() > 0)
      {
        // Sort imports by fully qualified name
        for (Name importName : new TreeSet<>(_importsByLocalName.values()))
        {
          if (!importName.getNamespace().equals(_namespace))
          {
            writeLine("import " + escapeIdentifier(importName.getFullName()));
          }
        }
        newline();
      }
    }
    writeInlineSchema(schema);
  }

  /**
   * Write a schema as inline code, not including any namespace, package or import preamble.
   *
   * @param schema provides the schema to write.
   */
  private void writeInlineSchema(DataSchema schema) throws IOException
  {
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
      default:
        throw new IllegalArgumentException("Unrecognized schema type " + schema.getClass());
    }
  }

  private void writeRecord(RecordDataSchema schema) throws IOException
  {
    writeDoc(schema.getDoc());
    writeProperties(schema.getProperties());
    write("record ");
    write(toTypeIdentifier(schema));
    List<NamedDataSchema> includes = schema.getInclude();
    if (includes.size() > 0)
    {
      write(" includes ");
      for (Iterator<NamedDataSchema> iter = includes.iterator(); iter.hasNext();)
      {
        NamedDataSchema include = iter.next();
        writeReferenceOrInline(include, schema.isIncludeDeclaredInline(include));
        if (iter.hasNext())
        {
          write(", ");
        }
      }
    }
    write(" {");
    newline();

    _indentDepth++;
    for (RecordDataSchema.Field field : schema.getFields())
    {
      if (field.getRecord().equals(schema))
      {
        writeDoc(field.getDoc());
        writeProperties(field.getProperties());
        indent();
        write(escapeIdentifier(field.getName()));
        write(": ");
        writeReferenceOrInline(field.getType(), field.isDeclaredInline());
        if (field.getOptional())
        {
          write("?");
        }

        if (field.getDefault() != null)
        {
          write(" = ");
          write(toJson(field.getDefault()));
        }
        newline();
      }
    }
    _indentDepth--;
    indent();
    write("}");
  }

  private void writeEnum(EnumDataSchema schema) throws IOException
  {
    writeDoc(schema.getDoc());
    DataMap properties = new DataMap(schema.getProperties());
    DataMap propertiesMap = new DataMap(coercePropertyToDataMapOrFail(schema, "symbolProperties", properties.remove("symbolProperties")));
    DataMap deprecatedMap = coercePropertyToDataMapOrFail(schema, "deprecatedSymbols", properties.remove("deprecatedSymbols"));
    writeProperties(properties);
    write("enum ");
    write(toTypeIdentifier(schema));
    write(" {");
    newline();

    _indentDepth++;
    Map<String, String> docs = schema.getSymbolDocs();

    for (String symbol : schema.getSymbols())
    {
      writeDoc(docs.get(symbol));
      DataMap symbolProperties = coercePropertyToDataMapOrFail(schema, "symbolProperties." + symbol, propertiesMap.get(symbol));
      Object deprecated = deprecatedMap.get(symbol);
      if (deprecated != null)
      {
        symbolProperties.put("deprecated", deprecated);
      }
      writeProperties(symbolProperties);
      writeLine(symbol);
    }
    _indentDepth--;
    indent();
    write("}");
  }

  private void writeFixed(FixedDataSchema schema) throws IOException
  {
    writeDoc(schema.getDoc());
    writeProperties(schema.getProperties());
    write("fixed ");
    write(toTypeIdentifier(schema));
    write(" ");
    write(String.valueOf(schema.getSize()));
  }

  private void writeTyperef(TyperefDataSchema schema) throws IOException
  {
    writeDoc(schema.getDoc());
    writeProperties(schema.getProperties());
    write("typeref ");
    write(toTypeIdentifier(schema));
    write(" = ");
    DataSchema ref = schema.getRef();
    writeReferenceOrInline(ref, schema.isRefDeclaredInline());
  }

  private void writeMap(MapDataSchema schema) throws IOException
  {
    write("map[string, ");
    writeReferenceOrInline(schema.getValues(), schema.isValuesDeclaredInline());
    write("]");
  }

  private void writeArray(ArrayDataSchema schema) throws IOException
  {
    write("array[");
    writeReferenceOrInline(schema.getItems(), schema.isItemsDeclaredInline());
    write("]");
  }

  private void writeUnion(UnionDataSchema schema) throws IOException
  {
    write("union[");
    for(Iterator<DataSchema> iter = schema.getTypes().iterator(); iter.hasNext();)
    {
      DataSchema member = iter.next();
      writeReferenceOrInline(member, schema.isTypeDeclaredInline(member));
      if (iter.hasNext())
      {
        write(", ");
      }
    }
    write("]");
  }

  private void writePrimitive(PrimitiveDataSchema schema) throws IOException
  {
    write(schema.getUnionMemberKey());
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
   * Write a documentation string to .pdl code.
   * The documentation string will be embedded in a properly indented javadoc style doc string delimiters and margin.
   * @param doc provides the documentation to write.
   */
  private void writeDoc(String doc) throws IOException
  {
    if (StringUtils.isNotBlank(doc))
    {
      writeLine("/**");

      for (String line : doc.split("\n"))
      {
        indent();
        write(" * ");
        write(line);
        newline();
      }
      writeLine(" */");
    }
  }

  /**
   * Serialize a pegasus Data binding type to JSON.
   * Valid types: DataList, DataMap, String, Int, Long, Float, Double, Boolean, ByteArray
   * @param value provides the value to serialize to JSON.
   * @return a JSON serialized string representation of the data value.
   */
  private String toJson(Object value) throws IOException
  {
    if (value instanceof DataMap)
    {
      return CODEC.mapToString((DataMap) value);
    }
    else if (value instanceof DataList)
    {
      return CODEC.listToString((DataList) value);
    }
    else if (value instanceof String)
    {
      return "\"" + StringEscapeUtils.escapeJson((String) value) + "\"";
    }
    else if (value instanceof Number)
    {
      return String.valueOf(value);
    }
    else if (value instanceof Boolean)
    {
      return String.valueOf(value);
    }
    else if (value instanceof ByteString)
    {
      return ((ByteString) value).asAvroString();
    }
    else
    {
      throw new IllegalArgumentException("Unsupported data type: " + value.getClass());
    }
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
    markEncountered(dataSchema);
    if (representation == TypeRepresentation.DECLARED_INLINE)
    {
      writeInlineSchema(dataSchema);
    }
    else
    {
      if (dataSchema instanceof NamedDataSchema)
      {
        write(toTypeIdentifier((NamedDataSchema) dataSchema));
      }
      else
      {
        throw new IllegalArgumentException("Unnamed not marked as inline: " + dataSchema);
      }
    }
  }

  /**
   * Writes a set of schema properties to .pdl.
   * @param properties provides the properties to write.
   */
  private void writeProperties(Map<String, Object> properties) throws IOException
  {
    writeProperties(Collections.emptyList(), properties);
  }

  /**
   * Writes a set of schema properties that share a common prefix to .pdl.
   * @param prefix provides the common prefix of all the properties.
   * @param properties provides the properties to write.
   */
  private void writeProperties(List<String> prefix, Map<String, Object> properties) throws IOException
  {
    for (Map.Entry<String, Object> entry : properties.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();
      ArrayList<String> pathParts = new ArrayList<>(prefix);
      pathParts.add(key);
      if (value instanceof DataMap)
      {
        // Favor @x.y.z = "value" property encoding style over @x = { "y": { "z": "value" } }
        writeProperties(pathParts, (DataMap) value);
      }
      else if (value instanceof DataList)
      {
        writeProperty(pathParts, CODEC.listToString((DataList) value));
      }
      else if (Boolean.TRUE.equals(value))
      {
          // Use shorthand for boolean true.  Instead of writing "@deprecated = true",
          // write "@deprecated".
          indent();
          write("@");
          write(pathToString(pathParts));
          newline();
      }
      else
      {
        writeProperty(pathParts, value);
      }
    }
  }

  /**
   * Write a property string to this encoder's writer.
   * @param path provides the property's full path.
   * @param value provides the property's value, it may be any valid pegasus Data binding type (DataList, DataMap,
   *              String, Int, Long, Float, Double, Boolean, ByteArray)
   */
  private void writeProperty(List<String> path, Object value) throws IOException
  {
    indent();
    write("@");
    write(pathToString(path));
    write(" = ");
    write(toJson(value));
    newline();
  }

  /**
   * Converts a property path list to an escaped .pdl path string.
   * @param path provide a property path list.
   * @return a escaped .pdl path string.
   */
  private String pathToString(List<String> path)
  {
    return path.stream().map(this::escapeIdentifier).collect(Collectors.joining("."));
  }

  /**
   * Calculates which types to import to minimize the need to fully qualify names in a .pdl source file.
   *
   * When multiple referenced types have the same unqualified name only one is imported using the following rules:
   * - Prefer types from the current namespace over types from other namespaces with colliding unqualified names.
   * - Prefer the first lexically encountered type.
   *
   * The resulting import list includes types from the current namespace. These should not be explicitly written
   * as import statements in the .pdl source, but are essential to keep in the import set to prevent collisions with
   * types from other namespaces.
   *
   * Any type that is not imported must be referenced by fully qualified name through the .pdl source.
   *
   * @param schema provide the top level schema to calculate imports for.
   * @return a sorted map of schema type names to import, keyed by local name.
   */
  private Map<String, Name> computeImports(DataSchema schema) throws IOException
  {
    Map<String, Name> imports = new HashMap<>();
    computeImports(schema, true, imports);
    return imports;
  }

  /**
   * See @{link computeImports}.
   *
   * @param schema provides a schema to search for referenced types.
   * @param isDeclaredInline true if the schema should be treated as an inline declaration, false if it should be
   *                         considered a by-name reference.
   * @param importsAcc provides an imports result accumulator.
   */
  private void computeImports(DataSchema schema, boolean isDeclaredInline, Map<String, Name> importsAcc) throws IOException
  {
    if (!isDeclaredInline)
    {
      if (schema instanceof NamedDataSchema)
      {
        NamedDataSchema namedSchema = (NamedDataSchema) schema;
        Name name = new Name(namedSchema.getFullName());
        if (name.getNamespace().equals(_namespace))
        {
          // Prefer importing types in the current namespace over types from other namespaces with colliding unqualified
          // names.
          importsAcc.put(name.getName(), name);
        }
        else
        {
          importsAcc.putIfAbsent(name.getName(), name);
        }
      }
    }
    else
    {
      if (schema instanceof RecordDataSchema)
      {
        RecordDataSchema recordSchema = (RecordDataSchema) schema;
        for (RecordDataSchema.Field field : recordSchema.getFields())
        {
          computeImports(field.getType(), field.isDeclaredInline(), importsAcc);
        }
        for (NamedDataSchema include : recordSchema.getInclude())
        {
          computeImports(include, true, importsAcc);
        }
      }
      else if (schema instanceof TyperefDataSchema)
      {
        TyperefDataSchema typerefSchema = (TyperefDataSchema) schema;
        computeImports(typerefSchema.getRef(), typerefSchema.isRefDeclaredInline(), importsAcc);
      }
      else if (schema instanceof UnionDataSchema)
      {
        UnionDataSchema unionSchema = (UnionDataSchema) schema;
        for (DataSchema member : unionSchema.getTypes())
        {
          computeImports(member, unionSchema.isTypeDeclaredInline(member), importsAcc);
        }
      }
      else if (schema instanceof MapDataSchema)
      {
        MapDataSchema mapSchema = (MapDataSchema) schema;
        computeImports(mapSchema.getValues(), mapSchema.isValuesDeclaredInline(), importsAcc);
      }
      else if (schema instanceof ArrayDataSchema)
      {
        ArrayDataSchema arraySchema = (ArrayDataSchema) schema;
        computeImports(arraySchema.getItems(), arraySchema.isItemsDeclaredInline(), importsAcc);
      }
    }
  }

  /**
   * Get the .pdl escaped source identifier for the given named type.
   * If the type is imported, it's simple name will be returned, else it's fully qualified name will be returned.
   *
   * @param schema provides the named schema to get a .pdl escaped source identifier for.
   * @return a escaped source identifier.
   */
  private String toTypeIdentifier(NamedDataSchema schema)
  {
    if (schema.getNamespace().equals(_namespace) ||
        (_importsByLocalName.containsKey(schema.getName()) &&
        _importsByLocalName.get(schema.getName()).getNamespace().equals(schema.getNamespace())))
    {
      return escapeIdentifier(schema.getName());
    }
    else
    {
      return escapeIdentifier(schema.getFullName());
    }
  }

  /**
   * Escape an identifier for use in .pdl source code, replacing all identifiers that would conflict with .pdl
   * keywords with a '`' escaped identifier. The identifier may be either qualified or unqualified.
   *
   * @param identifier provides the identifier to escape.
   * @return an escaped identifier for use in .pdl source code.
   */
  private String escapeIdentifier(String identifier)
  {
    return Arrays.stream(identifier.split("\\.")).map(part -> {
      if (KEYWORDS.contains(part))
      {
        return '`' + part.trim() + '`';
      }
      else
      {
        return part.trim();
      }
    }).collect(Collectors.joining("."));
  }

  /**
   * Write an intended line of .pdl code.
   * The code will be prefixed by the current indentation and suffixed with a newline.
   * @param code provide the line of .pdl code.
   */
  private void writeLine(String code) throws IOException
  {
    indent();
    write(code);
    newline();
  }

  /**
   * Writes the current indentation as .pdl source.
   * Typically used in conjunction with write() and newline() to emit an entire line of .pdl source.
   */
  private void indent() throws IOException
  {
    for (int i = 0; i < _indentDepth; i++)
    {
      _out.write("  ");
    }
  }

  /**
   * Write a fragment of .pdl code.
   * The code fragment will be written verbatim.
   * @param codeFragment provides the fragment to write.
   */
  private void write(String codeFragment) throws IOException
  {
    _out.write(codeFragment);
  }

  /**
   * Write a newline as .pdl source.
   * Typically used in conjunction with indent() and write() to emit an entire line of .pdl source.
   */
  private void newline() throws IOException
  {
    _out.write(System.lineSeparator());
  }
}
