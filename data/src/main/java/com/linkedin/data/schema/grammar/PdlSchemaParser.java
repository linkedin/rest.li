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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.grammar.PdlLexer;
import com.linkedin.data.grammar.PdlParser;
import com.linkedin.data.grammar.PdlParser.AnonymousTypeDeclarationContext;
import com.linkedin.data.grammar.PdlParser.ArrayDeclarationContext;
import com.linkedin.data.grammar.PdlParser.DocumentContext;
import com.linkedin.data.grammar.PdlParser.EnumDeclarationContext;
import com.linkedin.data.grammar.PdlParser.EnumSymbolDeclarationContext;
import com.linkedin.data.grammar.PdlParser.FieldDeclarationContext;
import com.linkedin.data.grammar.PdlParser.FieldDefaultContext;
import com.linkedin.data.grammar.PdlParser.FieldSelectionContext;
import com.linkedin.data.grammar.PdlParser.FixedDeclarationContext;
import com.linkedin.data.grammar.PdlParser.ImportDeclarationContext;
import com.linkedin.data.grammar.PdlParser.ImportDeclarationsContext;
import com.linkedin.data.grammar.PdlParser.JsonValueContext;
import com.linkedin.data.grammar.PdlParser.MapDeclarationContext;
import com.linkedin.data.grammar.PdlParser.NamedTypeDeclarationContext;
import com.linkedin.data.grammar.PdlParser.ObjectEntryContext;
import com.linkedin.data.grammar.PdlParser.PropDeclarationContext;
import com.linkedin.data.grammar.PdlParser.RecordDeclarationContext;
import com.linkedin.data.grammar.PdlParser.ScopedNamedTypeDeclarationContext;
import com.linkedin.data.grammar.PdlParser.TypeAssignmentContext;
import com.linkedin.data.grammar.PdlParser.TypeDeclarationContext;
import com.linkedin.data.grammar.PdlParser.TypeReferenceContext;
import com.linkedin.data.grammar.PdlParser.TyperefDeclarationContext;
import com.linkedin.data.grammar.PdlParser.UnionDeclarationContext;
import com.linkedin.data.grammar.PdlParser.UnionMemberAliasContext;
import com.linkedin.data.grammar.PdlParser.UnionMemberDeclarationContext;
import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.ComplexDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.RecordDataSchema.Field;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.exception.ExceptionUtils;


/**
 * Parses pegasus schema language source (.pdl) and generates Pegasus DataSchema types.
 *
 * @author Joe Betz
 */
public class PdlSchemaParser extends AbstractSchemaParser
{
  public static final String FILETYPE = "pdl";
  public static final String FILE_EXTENSION = '.' + FILETYPE;

  private static final String NEWLINE = System.lineSeparator();

  // Mapping from simple name to full name
  private Map<String, Name> _currentImports;
  private final boolean _isLocationNeeded;
  private final Map<Object, ParseLocation> _parseLocations;

  private final StringBuilder _errorMessageBuilder = new StringBuilder();

  public PdlSchemaParser(DataSchemaResolver resolver)
  {
    this(resolver, false);
  }

  /**
   * Construct PDL parser with the option to return context locations of schema elements after parsings.
   *
   * @param resolver Schema resolver to use to resolve referenced schemas in the source text.
   * @param returnContextLocations Enable recording the context locations of schema elements during parsing. The
   *                              locations can be retrieved using {@link #getParseLocations()} after parsing.
   */
  public PdlSchemaParser(DataSchemaResolver resolver, boolean returnContextLocations)
  {
    super(resolver);
    this._isLocationNeeded = returnContextLocations;
    if (returnContextLocations)
    {
      this._parseLocations = new IdentityHashMap<>();
    }
    else
    {
      this._parseLocations = Collections.emptyMap();
    }

  }

  /**
   * Parse a representation of a schema from source.
   *
   * The top level {{DataSchema}'s parsed are in {{#topLevelDataSchemas}.
   * These are the types that are not defined within other types.
   * Parse errors are in {{#errorMessageBuilder} and indicated
   * by {{#hasError()}.
   *
   * @param source with the source code representation of the schema.
   */
  public void parse(String source)
  {
    parse(new StringReader(source));
  }

  /**
   * Parse a JSON representation of a schema from an {{java.io.InputStream}}.
   *
   * The top level {{DataSchema}}'s parsed are in {{#topLevelDataSchemas}}.
   * These are the types that are not defined within other types.
   * Parse errors are in {{#errorMessageBuilder}} and indicated
   * by {{#hasError()}}.
   *
   * @param inputStream with the JSON representation of the schema.
   */
  public void parse(InputStream inputStream)
  {
    parse(new InputStreamReader(inputStream));
  }

  /**
   * Parse a JSON representation of a schema from a {{java.io.Reader}}.
   *
   * The top level {{DataSchema}}'s parsed are in {{#topLevelDataSchemas}}.
   * These are the types that are not defined within other types.
   * Parse errors are in {{#errorMessageBuilder}} and indicated
   * by {{#hasError()}}.
   *
   * @param reader with the JSON representation of the schema.
   */
  public void parse(Reader reader)
  {
    try
    {
      ErrorRecorder errorRecorder = new ErrorRecorder();
      PdlLexer lexer;
      try
      {
        lexer = new PdlLexer(new ANTLRInputStream(reader));
      }
      catch (IOException e)
      {
        ParseError error = new ParseError(new ParseErrorLocation(0, 0), e.getMessage());
        startErrorMessage(error).append(error.message).append(NEWLINE);
        return;
      }
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorRecorder);

      PdlParser parser = new PdlParser(new CommonTokenStream(lexer));
      parser.removeErrorListeners();
      parser.addErrorListener(errorRecorder);

      DocumentContext antlrDocument = parser.document();
      parse(antlrDocument);

      if (errorRecorder.errors.size() > 0)
      {
        for (ParseError error : errorRecorder.errors)
        {
          startErrorMessage(error).append(error.message).append(NEWLINE);
        }
      }
    }
    catch (ParseException e)
    {
      startErrorMessage(e.error).append(e.getMessage()).append(NEWLINE);
    }
    catch (Throwable t)
    {
      ParseError parseError = new ParseError(new ParseErrorLocation(0, 0), null);
      startErrorMessage(parseError).append("Unexpected parser error: ").append(ExceptionUtils.getStackTrace(t)).append(NEWLINE);
    }
  }

  /**
   * Returns the context locations of the entities that were parsed from the document. Parse should be created with
   * {@link #PdlSchemaParser(DataSchemaResolver, boolean)} and by specifying "true" for returnContextLocations. Otherwise
   * the returned map will be empty.
   *
   * Locations for the following elements defined in the source file will be returned:
   * <ul>
   *   <li>All named schemas: Records, Enums, Fixed and Typerefs </li>
   *   <li>Fields of records (fields from includes are not handled).</li>
   *   <li>Union schemas and all union members</li>
   *   <li>Enum symbols</li>
   *   <li>Top level and inline namespaces</li>
   * </ul>
   *
   * The returned location {@link ParseLocation} will provide the start line/column and end line/column. All four
   * of these positions are inclusive. The column indexes start with 1, that is, the first character in a line will be
   * at position 1.
   */
  public Map<Object, ParseLocation> getParseLocations() {
    return _parseLocations;
  }

  private StringBuilder startErrorMessage(ParseError error)
  {
    return errorMessageBuilder().append(error.location).append(": ");
  }

  private StringBuilder startErrorMessage(ParserRuleContext context)
  {
    return errorMessageBuilder().append(new ParseErrorLocation(context)).append(": ");
  }

  /**
   * An ANTLR lexer or parser error.
   */
  private static class ParseError
  {
    public final ParseErrorLocation location;
    public final String message;

    public ParseError(ParseErrorLocation location, String message)
    {
      this.location = location;
      this.message = message;
    }
  }

  /**
   * Represents the location of a schema element in the source text.
   */
  public static class ParseLocation
  {
    int _startLine;
    int _startColumn;
    int _endLine;
    int _endColumn;

    public ParseLocation(int startLine, int startColumn, int endLine, int endColumn)
    {
      _startLine = startLine;
      _startColumn = startColumn;
      _endLine = endLine;
      _endColumn = endColumn;
    }

    /**
     * Returns the  1-indexed, inclusive start line of the schema element.
     */
    public int getStartLine() {
      return _startLine;
    }

    /**
     * Returns the  1-indexed, inclusive start column of the element.
     */
    public int getStartColumn() {
      return _startColumn;
    }

    /**
     * Returns the 1-indexed, inclusive end line of the schema element.
     */
    public int getEndLine() {
      return _endLine;
    }

    /**
     * Returns the 1-indexed, inclusive end column of the element.
     */
    public int getEndColumn() {
      return _endColumn;
    }
  }

  /**
   * Pegasus DataLocation implementation for tracking ANTLR lexer and parser error source
   * coordinates.
   */
  private static class ParseErrorLocation implements DataLocation
  {
    public final int line;
    public final int column;

    public ParseErrorLocation(ParserRuleContext context)
    {
      Token start = context.getStart();
      this.line = start.getLine();
      this.column = start.getCharPositionInLine();
    }

    public ParseErrorLocation(int line, int column)
    {
      this.line = line;
      this.column = column;
    }

    @Override
    public int compareTo(DataLocation location)
    {
      if (!(location instanceof ParseErrorLocation))
      {
        return -1;
      }
      else
      {
        ParseErrorLocation other = (ParseErrorLocation)location;
        int lineCompare = this.line - other.line;
        if (lineCompare != 0)
        {
          return lineCompare;
        }
        return this.column - other.column;
      }
    }

    @Override
    public String toString()
    {
      return line + "," + column;
    }
  }

  /**
   * An exceptional parse error.  Should only be thrown for parse errors that are unrecoverable,
   * i.e. errors forcing the parser to must halt immediately and not continue to parse the
   * document in search of other potential errors to report.
   *
   * For recoverable parse errors, the error should instead be recorded using startErrorMessage.
   */
  protected static class ParseException extends IOException
  {
    private static final long serialVersionUID = 1;

    public final ParseError error;

    public ParseException(ParserRuleContext context, String msg)
    {
      this(new ParseError(new ParseErrorLocation(context), msg));
    }
    public ParseException(ParseError error)
    {
      super(error.message);
      this.error = error;
    }
  }

  /**
   * Error recorder to capture ANTLR lexer and parser errors.
   */
  private static class ErrorRecorder extends BaseErrorListener
  {
    public final List<ParseError> errors = new LinkedList<>();

    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int column,
                            String msg,
                            RecognitionException e)
    {
      ParseErrorLocation location = new ParseErrorLocation(line, column);
      errors.add(new ParseError(location, msg));
    }
  }

  /**
   * Parse list of Data objects.
   *
   * The {{DataSchema}'s parsed are in {{#topLevelDataSchemas}.
   * Parse errors are in {{#errorMessageBuilder} and indicated
   * by {{#hasError()}.
   *
   * @param document provides the source code in AST form
   */
  private DataSchema parse(DocumentContext document) throws ParseException
  {
    // Set root namespace
    PdlParser.NamespaceDeclarationContext namespaceDeclaration = document.namespaceDeclaration();
    if (namespaceDeclaration != null)
    {
      setCurrentNamespace(namespaceDeclaration.typeName().value);
      recordLocation(namespaceDeclaration.typeName().value, namespaceDeclaration);
    }
    else
    {
      setCurrentNamespace("");
    }

    // Set root package
    PdlParser.PackageDeclarationContext packageDeclaration = document.packageDeclaration();
    if (packageDeclaration != null)
    {
      setCurrentPackage(packageDeclaration.typeName().value);
    }
    else
    {
      setCurrentPackage(null);
    }

    setCurrentImports(document.importDeclarations(), getCurrentNamespace());
    TypeDeclarationContext typeDeclaration = document.typeDeclaration();
    DataSchema schema;
    if (typeDeclaration.namedTypeDeclaration() != null)
    {
      NamedDataSchema namedSchema = parseNamedType(typeDeclaration.namedTypeDeclaration());
      if (!namedSchema.getNamespace().equals(getCurrentNamespace()))
      {
        throw new ParseException(typeDeclaration,
            "Top level type declaration may not be qualified with a namespace different than the file namespace: "
                + typeDeclaration.getText());
      }
      schema = namedSchema;
    }
    else if (typeDeclaration.anonymousTypeDeclaration() != null)
    {
      schema = parseAnonymousType(typeDeclaration.anonymousTypeDeclaration());
    }
    else
    {
      throw new ParseException(typeDeclaration, "Unrecognized type declaration: " + typeDeclaration.getText());
    }
    addTopLevelSchema(schema);
    return schema;
  }

  private DataSchema parseType(TypeDeclarationContext type) throws ParseException
  {
    if (type.scopedNamedTypeDeclaration() != null)
    {
      return parseScopedNamedType(type.scopedNamedTypeDeclaration());
    }
    if (type.namedTypeDeclaration() != null)
    {
      return parseNamedType(type.namedTypeDeclaration());
    }
    else if (type.anonymousTypeDeclaration() != null)
    {
      return parseAnonymousType(type.anonymousTypeDeclaration());
    }
    else
    {
      throw new ParseException(type, "Unrecognized type declaration parse node: " + type.getText());
    }
  }

  private DataSchema parseScopedNamedType(ScopedNamedTypeDeclarationContext type) throws ParseException {
    String surroundingNamespace = getCurrentNamespace();
    String surroundingPackage = getCurrentPackage();

    PdlParser.NamespaceDeclarationContext scopeNamespace = type.namespaceDeclaration();
    if (scopeNamespace != null) {
      setCurrentNamespace(scopeNamespace.typeName().value);
      recordLocation(scopeNamespace.typeName().value, scopeNamespace);
    }
    PdlParser.PackageDeclarationContext scopePackage = type.packageDeclaration();
    if (scopePackage != null) {
      setCurrentPackage(scopePackage.typeName().value);
    }

    NamedDataSchema parsedType = parseNamedType(type.namedTypeDeclaration());

    setCurrentNamespace(surroundingNamespace);
    setCurrentPackage(surroundingPackage);

    return parsedType;
  }

  private DataSchema parseAnonymousType(AnonymousTypeDeclarationContext anon) throws ParseException {
    ComplexDataSchema complexDataSchema;
    if (anon.unionDeclaration() != null)
    {
      complexDataSchema =  parseUnion(anon.unionDeclaration(), false);
    }
    else if (anon.mapDeclaration() != null)
    {
      complexDataSchema = parseMap(anon.mapDeclaration());
    }
    else if (anon.arrayDeclaration() != null)
    {
      complexDataSchema = parseArray(anon.arrayDeclaration());
    }
    else
    {
      throw new ParseException(anon, "Unrecognized type parse node: " + anon.getText());
    }
    setProperties(anon, complexDataSchema);
    recordLocation(complexDataSchema, anon);
    return complexDataSchema;
  }

  private NamedDataSchema parseNamedType(
      NamedTypeDeclarationContext namedType) throws ParseException
  {
    NamedDataSchema schema;
    if (namedType.recordDeclaration() != null)
    {
      schema = parseRecord(namedType, namedType.recordDeclaration());
    }
    else if (namedType.typerefDeclaration() != null)
    {
      schema = parseTyperef(namedType, namedType.typerefDeclaration());
    }
    else if (namedType.fixedDeclaration() != null)
    {
      schema = parseFixed(namedType, namedType.fixedDeclaration());
    }
    else if (namedType.enumDeclaration() != null)
    {
      schema = parseEnum(namedType, namedType.enumDeclaration());
    }
    else
    {
      throw new ParseException(namedType, "Unrecognized named type parse node: " + namedType.getText());
    }

    if (_currentImports.containsKey(schema.getName()))
    {
      final Name importName = _currentImports.get(schema.getName());
      if (importName.getFullName().equals(schema.getFullName()))
      {
        // Prohibit importing types that are declared in the same document
        startErrorMessage(namedType)
            .append("Import '")
            .append(schema.getFullName())
            .append("' references a type declared in the same document. Please remove it.")
            .append(NEWLINE);
      }
      else
      {
        // Prohibit declaring types that conflict with imported types
        startErrorMessage(namedType)
            .append("Declaration of type '")
            .append(schema.getFullName())
            .append("' conflicts with import '")
            .append(importName.getFullName())
            .append("'. Please remove the import and instead use its fully qualified name to avoid ambiguity.")
            .append(NEWLINE);
      }
    }

    schema.setPackage(getCurrentPackage());
    recordLocation(schema, namedType);
    return schema;
  }

  /**
   * Stores the location of the schema element obtained from the parser context. See {@link #getParseLocations()}
   * for the full list of schema elements for which locations are recorded/returned.
   */
  private void recordLocation(Object schemaElement, ParserRuleContext context)
  {
    if (_isLocationNeeded)
    {
      // getCharPosition returns beginning of the last token. Add the token's length to get actual end position.
      int endPosition = context.getStop().getCharPositionInLine() +
          (context.getStop().getStopIndex() - context.getStop().getStartIndex());
      // Parser columns are indexed at 0, so add 1 to get it 1 indexed.
      _parseLocations.put(schemaElement,
          new ParseLocation(context.getStart().getLine(), context.getStart().getCharPositionInLine() + 1,
              context.getStop().getLine(), endPosition + 1));
    }
  }

  private FixedDataSchema parseFixed(
      NamedTypeDeclarationContext context,
      FixedDeclarationContext fixed) throws ParseException
  {
    Name name = toName(fixed.name);
    FixedDataSchema schema = new FixedDataSchema(name);

    setDocAndProperties(context, schema);
    bindNameToSchema(name, schema.getAliases(), schema);

    schema.setSize(fixed.size, errorMessageBuilder());
    return schema;
  }

  private EnumDataSchema parseEnum(
      NamedTypeDeclarationContext context,
      EnumDeclarationContext enumDecl) throws ParseException
  {
    Name name = toName(enumDecl.name);
    EnumDataSchema schema = new EnumDataSchema(name);

    // This is useful to set the doc and the aliases, but the properties are overwritten later (see below)
    Map<String, Object> props = setDocAndProperties(context, schema);
    bindNameToSchema(name, schema.getAliases(), schema);

    List<EnumSymbolDeclarationContext> symbolDecls = enumDecl.enumDecl.symbolDecls;

    List<String> symbols = new ArrayList<>(symbolDecls.size());
    Map<String, Object> symbolDocs = new HashMap<>();
    DataMap deprecatedSymbols = new DataMap();
    DataMap symbolProperties = new DataMap();

    for (EnumSymbolDeclarationContext symbolDecl : symbolDecls)
    {
      symbols.add(symbolDecl.symbol.value);
      recordLocation(symbolDecl.symbol.value, symbolDecl);
      if (symbolDecl.doc != null)
      {
        symbolDocs.put(symbolDecl.symbol.value, symbolDecl.doc.value);
      }
      for (PropDeclarationContext prop: symbolDecl.props)
      {
        String symbol = symbolDecl.symbol.value;
        Object value = parsePropValue(prop);
        if (equalsSingleSegmentProperty(prop, DataSchemaConstants.DEPRECATED_KEY))
        {
          deprecatedSymbols.put(symbol, value);
        }
        else
        {
          List<String> path = new ArrayList<>(prop.path.size() + 1);
          path.add(symbol);
          path.addAll(prop.path);
          addPropertiesAtPath(prop, symbolProperties, path, value);
        }
      }
    }

    schema.setSymbols(symbols, errorMessageBuilder());
    if (!symbolDocs.isEmpty())
    {
      schema.setSymbolDocs(symbolDocs, errorMessageBuilder());
    }

    if (!deprecatedSymbols.isEmpty())
    {
      props.put(DataSchemaConstants.DEPRECATED_SYMBOLS_KEY, deprecatedSymbols);
    }

    if (!symbolProperties.isEmpty())
    {
      props.put(DataSchemaConstants.SYMBOL_PROPERTIES_KEY, symbolProperties);
    }

    // Overwrite the properties now that we've computed the special symbol properties
    schema.setProperties(props);

    return schema;
  }

  private TyperefDataSchema parseTyperef(
      NamedTypeDeclarationContext context,
      TyperefDeclarationContext typeref) throws ParseException
  {
    Name name = toName(typeref.name);
    TyperefDataSchema schema = new TyperefDataSchema(name);
    getResolver().addPendingSchema(schema.getFullName());
    try
    {
      setDocAndProperties(context, schema);
      bindNameToSchema(name, schema.getAliases(), schema);
      DataSchema refSchema = toDataSchema(typeref.ref);
      checkTyperefCycle(schema, refSchema);
      schema.setReferencedType(refSchema);
      schema.setRefDeclaredInline(isDeclaredInline(typeref.ref));
    }
    finally
    {
      getResolver().removePendingSchema(schema.getFullName());
    }
    return schema;
  }

  private ArrayDataSchema parseArray(ArrayDeclarationContext array) throws ParseException
  {
    ArrayDataSchema schema = new ArrayDataSchema(toDataSchema(array.typeParams.items));
    schema.setItemsDeclaredInline(isDeclaredInline(array.typeParams.items));
    return schema;
  }

  private MapDataSchema parseMap(MapDeclarationContext map) throws ParseException
  {
    TypeAssignmentContext keyType = map.typeParams.key;
    TypeAssignmentContext valueType = map.typeParams.value;
    MapDataSchema schema = new MapDataSchema(toDataSchema(valueType));
    Map<String, Object> propsToAdd = new HashMap<>();

    if (keyType.typeReference() != null)
    {
      String typeName = keyType.typeReference().value;

      if (!typeName.equals("string"))
      {
        startErrorMessage(map)
            .append("Unsupported map key type: ").append(typeName)
            .append(". 'string' is the only currently supported map key type.\n");

        // TODO(jbetz):
        // Support typed map keys once https://github.com/linkedin/rest.li/pull/61 is accepted.
        //String qualifiedKeyName = computeFullName(typeName);
        //propsToAdd.put("keys", qualifiedKeyName);
      }
    }
    else if (keyType.typeDeclaration() != null)
    {
      DataSchema keySchema = parseType(keyType.typeDeclaration());
      String json = SchemaToJsonEncoder.schemaToJson(keySchema, JsonBuilder.Pretty.COMPACT);
      startErrorMessage(map)
          .append("Unsupported map key type declaration: ").append(json)
          .append(". 'string' is the only currently supported map key type.\n");

      // TODO(jbetz):
      // Support typed map keys once https://github.com/linkedin/rest.li/pull/61 is accepted.
      //DataMap dataMap = codec.stringToMap(json);
      //propsToAdd.put("keys", dataMap);
    }

    schema.setProperties(propsToAdd);
    schema.setValuesDeclaredInline(isDeclaredInline(valueType));
    return schema;
  }

  private UnionDataSchema parseUnion(
      UnionDeclarationContext union, boolean withinTypref) throws ParseException
  {
    UnionDataSchema schema = new UnionDataSchema();
    List<UnionMemberDeclarationContext> members = union.typeParams.members;
    List<UnionDataSchema.Member> unionMembers = new ArrayList<>(members.size());
    for (UnionMemberDeclarationContext memberDecl: members)
    {
      // Get union member type assignment
      TypeAssignmentContext memberType = memberDecl.member;
      DataSchema dataSchema = toDataSchema(memberType);
      if (dataSchema != null)
      {
        UnionDataSchema.Member unionMember = new UnionDataSchema.Member(dataSchema);
        recordLocation(unionMember, memberDecl);
        unionMember.setDeclaredInline(isDeclaredInline(memberDecl.member));
        // Get union member alias, if any
        UnionMemberAliasContext alias = memberDecl.unionMemberAlias();
        if (alias != null)
        {
          // Set union member alias
          boolean isAliasValid = unionMember.setAlias(alias.name.value, startCalleeMessageBuilder());
          if (!isAliasValid)
          {
            appendCalleeMessage(unionMember);
          }
          // Set union member docs and properties
          if (alias.doc != null)
          {
            unionMember.setDoc(alias.doc.value);
          }
          final Map<String, Object> properties = new HashMap<>();
          for (PropDeclarationContext prop: alias.props)
          {
            addPropertiesAtPath(properties, prop);
          }
          unionMember.setProperties(properties);
        }
        unionMembers.add(unionMember);
      }
    }
    schema.setMembers(unionMembers, errorMessageBuilder());
    return schema;
  }

  private RecordDataSchema parseRecord(
      NamedTypeDeclarationContext context,
      RecordDeclarationContext record) throws ParseException
  {
    Name name = toName(record.name);
    RecordDataSchema schema = new RecordDataSchema(name, RecordDataSchema.RecordType.RECORD);

    getResolver().addPendingSchema(schema.getFullName());
    try
    {
      setDocAndProperties(context, schema);
      bindNameToSchema(name, schema.getAliases(), schema);
      FieldsAndIncludes fieldsAndIncludes = parseIncludes(schema, record.beforeIncludes);
      boolean hasBeforeIncludes = fieldsAndIncludes.includes.size() > 0;
      fieldsAndIncludes.fields.addAll(parseFields(schema, record.recordDecl));
      FieldsAndIncludes afterIncludes = parseIncludes(schema, record.afterIncludes);
      boolean hasAfterIncludes = afterIncludes.includes.size() > 0;
      if (hasBeforeIncludes && hasAfterIncludes)
      {
        startErrorMessage(record).append("Record may have includes before or after fields, but not both: ")
            .append(record)
            .append(NEWLINE);
      }
      fieldsAndIncludes.addAll(afterIncludes);
      schema.setFields(fieldsAndIncludes.fields, errorMessageBuilder());
      schema.setInclude(fieldsAndIncludes.includes);
      schema.setIncludesDeclaredInline(fieldsAndIncludes.includesDeclaredInline);
      schema.setFieldsBeforeIncludes(hasAfterIncludes);
      validateDefaults(schema);
    }
    finally
    {
      getResolver().removePendingSchema(schema.getFullName());
    }
    return schema;
  }

  /**
   * Sets doc, properties, and aliases on the provided {@link NamedDataSchema} using data parsed from the provided
   * {@link NamedTypeDeclarationContext}.
   *
   * @param source source to read doc, properties, and aliases from
   * @param target target on which to set doc, properties, and aliases
   * @return parsed properties
   */
  protected Map<String, Object> setDocAndProperties(NamedTypeDeclarationContext source, NamedDataSchema target)
      throws ParseException
  {
    Map<String, Object> properties = new HashMap<>(target.getProperties());

    if (source.doc != null)
    {
      target.setDoc(source.doc.value);
    }

    for (PropDeclarationContext prop: source.props)
    {
      if (equalsSingleSegmentProperty(prop, DataSchemaConstants.ALIASES_KEY))
      {
        List<Name> aliases = parseAliases(prop).stream()
            .map(this::toName)
            .collect(Collectors.toList());
        target.setAliases(aliases);
      }
      else
      {
        addPropertiesAtPath(properties, prop);
      }
    }

    target.setProperties(properties);
    return properties;
  }

  /**
   * Sets properties on the provided {@link ComplexDataSchema} using data parsed from the provided
   * {@link AnonymousTypeDeclarationContext}.
   *
   * @param source source to read properties from
   * @param target target on which to set properties
   */
  private void setProperties(AnonymousTypeDeclarationContext source, ComplexDataSchema target)
      throws ParseException
  {
    Map<String, Object> properties = new HashMap<>(target.getProperties());

    for (PropDeclarationContext prop: source.props)
    {
      addPropertiesAtPath(properties, prop);
    }

    target.setProperties(properties);
  }

  /**
   * Adds additional properties to an existing properties map at the location identified by
   * the given PropDeclarationContexts path.
   *
   * @param existingProperties provides the existing properties to add the additional properties to.
   * @param prop provides the ANTLR property AST node to add the properties for.
   */
  private void addPropertiesAtPath(
      Map<String, Object> existingProperties, PropDeclarationContext prop) throws ParseException
  {
    addPropertiesAtPath(prop, existingProperties, prop.path, parsePropValue(prop));
  }

  /**
   * Adds additional properties to an existing properties map at the location identified by
   * the given path.
   *
   * This allows for properties defined with paths such as:
   *
   * {@literal @}a.b = "x"
   * {@literal @}a.c = "y"
   *
   * to be merged together into a property map like:
   *
   * { "a": { "b": "x", "c": "y" }}
   *
   * Examples:
   *
   * <pre>
   * existing properties        | path  | value         | result
   * ---------------------------|-------|---------------|----------------------------------------
   * {}                         | a.b.c | true          | { "a": { "b": { "c": true } } }
   * { "a": {} }                | a.b   | true          | { "a": { "b": true } }
   * { "a": {} }                | a.b   | { "z": "x" }  | { "a": { "b": { "z": "x" } } }
   * { "a": { "c": "x"}} }      | a.b   | true          | { "a": { "b": true, "c": "x"} } }
   * { "a": { "b": "x"}} }      | a.b   | "y"           | ParseError "Conflicting property: a.b"
   * </pre>
   *
   * The existing properties are traversed using the given path, adding DataMaps as needed to
   * complete the traversal. If any of data elements in the existing properties along the path are
   * not DataMaps, a ParseError is thrown to report the conflict.
   *
   * @param context provides the parsing context for error reporting purposes.
   * @param existingProperties provides the properties to add to.
   * @param path provides the path of the property to insert.
   * @param value provides the value of the property to insert.
   * @throws ParseException if the path of the properties to add conflicts with data already
   * in the properties map or if a property is already exists at the path.
   */
  private void addPropertiesAtPath(
      ParserRuleContext context,
      Map<String, Object> existingProperties,
      Iterable<String> path,
      Object value) throws ParseException
  {
    Map<String, Object> current = existingProperties;
    Iterator<String> iter = path.iterator();
    while (iter.hasNext())
    {
      String pathPart = iter.next();
      if (iter.hasNext())
      {
        if (current.containsKey(pathPart))
        {
          Object val = current.get(pathPart);
          if (!(val instanceof DataMap))
          {
            throw new ParseException(
                new ParseError(new ParseErrorLocation(context), "Conflicting property: " + path.toString()));
          }
          current = (DataMap) val;
        }
        else
        {
          DataMap next = new DataMap();
          current.put(pathPart, next);
          current = next;
        }
      }
      else
      {
        if (current.containsKey(pathPart))
        {
          throw new ParseException(
              new ParseError(new ParseErrorLocation(context), "Property already defined: " + path.toString()));
        }
        else
        {
          current.put(pathPart, value);
        }
      }
    }
  }

  private static class FieldsAndIncludes
  {
    public final List<Field> fields;
    public final List<NamedDataSchema> includes;
    public final Set<NamedDataSchema> includesDeclaredInline;

    public FieldsAndIncludes(List<Field> fields, List<NamedDataSchema> includes, Set<NamedDataSchema> includesDeclaredInline)
    {
      this.fields = fields;
      this.includes = includes;
      this.includesDeclaredInline = includesDeclaredInline;
    }

    public void addAll(FieldsAndIncludes includes)
    {
      this.fields.addAll(includes.fields);
      this.includes.addAll(includes.includes);
      this.includesDeclaredInline.addAll(includes.includesDeclaredInline);
    }
  }

  private FieldsAndIncludes parseIncludes(RecordDataSchema recordDataSchema,
      PdlParser.FieldIncludesContext includeSet) throws ParseException
  {
    List<NamedDataSchema> includes = new ArrayList<>();
    Set<NamedDataSchema> includesDeclaredInline = new HashSet<>();
    List<Field> fields = new ArrayList<>();
    if (includeSet != null)
    {
      getResolver().updatePendingSchema(recordDataSchema.getFullName(), true);
      List<TypeAssignmentContext> includeTypes = includeSet.typeAssignment();
      for (TypeAssignmentContext includeRef : includeTypes)
      {
        DataSchema includedSchema = toDataSchema(includeRef);
        if (includedSchema != null)
        {
          DataSchema dereferencedIncludedSchema = includedSchema.getDereferencedDataSchema();
          if (includedSchema instanceof NamedDataSchema &&
              dereferencedIncludedSchema instanceof RecordDataSchema)
          {
            NamedDataSchema includedNamedSchema = (NamedDataSchema) includedSchema;
            RecordDataSchema dereferencedIncludedRecordSchema = (RecordDataSchema) dereferencedIncludedSchema;
            fields.addAll(dereferencedIncludedRecordSchema.getFields());
            includes.add(includedNamedSchema);
            if (isDeclaredInline(includeRef))
            {
              includesDeclaredInline.add(includedNamedSchema);
            }
          }
          else
          {
            startErrorMessage(includeRef)
                .append("Include is not a record type or a typeref to a record type: ")
                .append(includeRef).append(NEWLINE);
          }
        }
        else
        {
          startErrorMessage(includeRef)
              .append("Unable to resolve included schema: ")
              .append(includeRef).append(NEWLINE);
        }
      }
      getResolver().updatePendingSchema(recordDataSchema.getFullName(), false);
    }
    return new FieldsAndIncludes(fields, includes, includesDeclaredInline);
  }

  private List<Field> parseFields(
      RecordDataSchema recordSchema,
      FieldSelectionContext fieldGroup) throws ParseException
  {
    List<Field> results = new ArrayList<>();
    for (FieldDeclarationContext field : fieldGroup.fields)
    {
      if (field != null)
      {
        if (field.type == null) {
          throw new IllegalStateException("type is missing for field: " + field.getText());
        }
        Field result = new Field(toDataSchema(field.type));
        recordLocation(result, field);
        Map<String, Object> properties = new HashMap<>();
        result.setName(field.name, errorMessageBuilder());
        result.setOptional(field.isOptional);

        FieldDefaultContext fieldDefault = field.fieldDefault();
        if (fieldDefault != null)
        {
          JsonValueContext defaultValue = fieldDefault.jsonValue();
          if (defaultValue != null)
          {
            result.setDefault(parseJsonValue(defaultValue));
          }
        }

        List<String> aliases = new ArrayList<>();
        RecordDataSchema.Field.Order sortOrder = null;
        for (PropDeclarationContext prop : field.props)
        {
          if (equalsSingleSegmentProperty(prop, DataSchemaConstants.ALIASES_KEY))
          {
            aliases = parseAliases(prop);
          }
          else if (equalsSingleSegmentProperty(prop, DataSchemaConstants.ORDER_KEY))
          {
            Object value = parsePropValue(prop);
            if (!(value instanceof String))
            {
              startErrorMessage(prop)
                  .append("'order' must be string, but found ")
                  .append(prop.getText()).append(NEWLINE);
            }
            else
            {
              String order = (String) value;
              try
              {
                sortOrder = RecordDataSchema.Field.Order.valueOf(order.toUpperCase());
              }
              catch (IllegalArgumentException exc)
              {
                startErrorMessage(order).append("\"").append(order).append("\" is an invalid sort order.\n");
              }
            }
          }
          else
          {
            addPropertiesAtPath(properties, prop);
          }
        }
        if (field.doc != null)
        {
          result.setDoc(field.doc.value);
        }
        if (aliases.size() > 0)
        {
          result.setAliases(aliases, errorMessageBuilder());
        }
        if (sortOrder != null)
        {
          result.setOrder(sortOrder);
        }
        result.setProperties(properties);
        result.setRecord(recordSchema);
        result.setDeclaredInline(isDeclaredInline(field.type));

        results.add(result);
      }
      else
      {
        startErrorMessage(field)
          .append("Unrecognized field element parse node: ")
          .append(field.getText()).append(NEWLINE);
      }
    }
    return results;
  }

  /**
   * Parse the aliases (as strings) from some property declaration which is assumed to be an "aliases" property.
   * @param prop property declaration
   * @return list of aliases as strings
   */
  private List<String> parseAliases(PropDeclarationContext prop) throws ParseException
  {
    assert equalsSingleSegmentProperty(prop, DataSchemaConstants.ALIASES_KEY);

    final List<String> aliases = new ArrayList<>();

    Object value = parsePropValue(prop);
    if (!(value instanceof DataList))
    {
      startErrorMessage(prop)
          .append("'aliases' must be a list, but found ")
          .append(prop.getText()).append(NEWLINE);
    }
    else
    {
      for (Object alias : (DataList) value) {
        if (!(alias instanceof String))
        {
          startErrorMessage(prop)
              .append("'aliases' list elements must be string, but found ")
              .append(alias.getClass())
              .append(" at ")
              .append(prop.getText()).append(NEWLINE);
        }
        else
        {
          aliases.add((String) alias);
        }
      }
    }

    return aliases;
  }

  /**
   * Checks if the property is a single segment property and if that segment matches the property key provided.
   */
  private boolean equalsSingleSegmentProperty(PropDeclarationContext prop, String propertyKey)
  {
    return prop.path.size() == 1 && prop.path.get(0).equals(propertyKey);
  }

  private boolean isDeclaredInline(TypeAssignmentContext assignment)
  {
    return assignment.typeReference() == null;
  }

  protected DataSchema toDataSchema(TypeReferenceContext typeReference) throws ParseException
  {
    DataSchema dataSchema = stringToDataSchema(typeReference.value);
    if (dataSchema != null)
    {
      return dataSchema;
    }
    else
    {
      startErrorMessage(typeReference)
        .append("Type not found: ")
        .append(typeReference.value).append(NEWLINE);
      // Pegasus is designed to track null data schema references as errors, so we intentionally
      // return null here.
      return null;
    }
  }

  private DataSchema toDataSchema(TypeAssignmentContext typeAssignment) throws ParseException {
    TypeReferenceContext typeReference = typeAssignment.typeReference();
    if (typeReference != null)
    {
      return toDataSchema(typeReference);
    }
    else if (typeAssignment.typeDeclaration() != null)
    {
      return parseType(typeAssignment.typeDeclaration());
    }
    else
    {
      throw new ParseException(typeAssignment,
          "Unrecognized type assignment parse node: " + typeAssignment.getText() + NEWLINE);
    }
  }

  private Name toName(String name)
  {
    if (name.contains("."))
    {
      return new Name(name, errorMessageBuilder());
    }
    else
    {
      return new Name(name, getCurrentNamespace(), errorMessageBuilder());
    }
  }

  private Object parsePropValue(
      PdlParser.PropDeclarationContext prop) throws ParseException
  {
    if (prop.propJsonValue() != null)
    {
      return parseJsonValue(prop.propJsonValue().jsonValue());
    }
    else
    {
      return Boolean.TRUE;
    }
  }

  private Object parseJsonValue(JsonValueContext jsonValue) throws ParseException
  {
    if (jsonValue.array() != null)
    {
      DataList dataList = new DataList();
      for (JsonValueContext item: jsonValue.array().jsonValue())
      {
        dataList.add(parseJsonValue(item));
      }
      return dataList;
    }
    else if (jsonValue.object() != null)
    {
      DataMap dataMap = new DataMap();
      for (ObjectEntryContext entry: jsonValue.object().objectEntry())
      {
        dataMap.put(entry.key.value, parseJsonValue(entry.value));
      }
      return dataMap;
    }
    else if (jsonValue.string() != null)
    {
      return jsonValue.string().value;
    }
    else if (jsonValue.number() != null)
    {
      Number numberValue = jsonValue.number().value;
      if (numberValue == null)
      {
        startErrorMessage(jsonValue)
          .append("'")
          .append(jsonValue.number().getText())
          .append("' is not a valid int, long, float or double.")
          .append(NEWLINE);
        return 0;
      }
      return numberValue;
    }
    else if (jsonValue.bool() != null)
    {
      return jsonValue.bool().value;
    }
    else if (jsonValue.nullValue() != null)
    {
      return Null.getInstance();
    }
    else
    {
      startErrorMessage(jsonValue)
        .append("Unrecognized JSON parse node: ")
        .append(jsonValue.getText())
        .append(NEWLINE);
      return Null.getInstance();
    }
  }

  // Extended fullname computation to handle imports
  public String computeFullName(String name) {
    String fullname;
    if (DataSchemaUtil.typeStringToPrimitiveDataSchema(name) != null)
    {
      fullname = name;
    }
    else if (Name.isFullName(name))
    {
      fullname = name; // already a fullname
    }
    else if (_currentImports.containsKey(name))
    {
      // imported names are higher precedence than names in current namespace
      fullname = _currentImports.get(name).getFullName();
    }
    else if (getCurrentNamespace().isEmpty())
    {
      fullname = name;
    }
    else
    {
      fullname = getCurrentNamespace() + "." + name; // assumed to be in current namespace
    }
    return fullname;
  }

  /**
   * Sets the imports that can be used while parsing this document.
   * @param imports import declaration context for the document.
   * @param rootNamespace root namespace of this document.
   */
  private void setCurrentImports(ImportDeclarationsContext imports, String rootNamespace)
  {
    Map<String, Name> importsBySimpleName = new HashMap<>();
    for (ImportDeclarationContext importDecl: imports.importDeclaration())
    {
      String importedFullname = importDecl.type.value;
      Name importedName = new Name(importedFullname);
      // Prohibit imports from the root namespace
      if (importedName.getNamespace().equals(rootNamespace))
      {
        startErrorMessage(importDecl)
            .append("Import '")
            .append(importedFullname)
            .append("' is from within the document's root namespace and is thus unnecessary. Please remove it.")
            .append(NEWLINE);
      }
      // Prohibit imports with conflicting simple names
      String importedSimpleName = importedName.getName();
      if (importsBySimpleName.containsKey(importedSimpleName))
      {
        startErrorMessage(importDecl)
            .append("Import '")
            .append(importedFullname)
            .append("' conflicts with import '")
            .append(importsBySimpleName.get(importedSimpleName).getFullName())
            .append("'. Please remove one and instead use its fully qualified name.")
            .append(NEWLINE);
      }
      importsBySimpleName.put(importedSimpleName, importedName);
    }
    this._currentImports = importsBySimpleName;
  }

  @Override
  public String schemasToString()
  {
    return SchemaToJsonEncoder.schemasToJson(topLevelDataSchemas(), JsonBuilder.Pretty.SPACES);
  }

  @Override
  public StringBuilder errorMessageBuilder()
  {
    return _errorMessageBuilder;
  }
}
