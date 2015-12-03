package com.linkedin.data.schema;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * A schema parser is used to parse data schemas for a particular source representation.
 */
public interface DataSchemaParser {

  /**
   * Parse a source representation of a schema from an {@link InputStream}.
   *
   * The top level {@link DataSchema}'s parsed are in {@link #topLevelDataSchemas}.
   * These are the types that are not defined within other types.
   * Parse errors are in {@link #errorMessageBuilder} and indicated
   * by {@link #hasError()}.
   *
   * @param inputStream with the source representation of the schema.
   */
  void parse(InputStream inputStream);

  /**
   * Parse a source representation of a schema from a {@link Reader}.
   *
   * The top level {@link DataSchema}'s parsed are in {@link #topLevelDataSchemas}.
   * These are the types that are not defined within other types.
   * Parse errors are in {@link #errorMessageBuilder} and indicated
   * by {@link #hasError()}.
   *
   * @param reader with the source representation of the schema.
   */
  void parse(Reader reader);

  /**
   * Parse a source representation of a schema from a {@link String}.
   *
   * The top level {@link DataSchema}'s parsed are in {@link #topLevelDataSchemas}.
   * These are the types that are not defined within other types.
   * Parse errors are in {@link #errorMessageBuilder} and indicated
   * by {@link #hasError()}.
   *
   * @param string with the source representation of the schema.
   */
  void parse(String string);

  /**
   * Get the {@link DataSchemaResolver}.
   *
   * @return the resolver to used to find {@link DataSchema}'s, may be null
   *         if no resolver has been provided to parser.
   */
  DataSchemaResolver getResolver();

  /**
   * Set the current location for the source of input to the parser.
   *
   * This current location is will be used to annotate {@link NamedDataSchema}'s
   * generated from parsing.
   *
   * @param location of the input source.
   */
  void setLocation(DataSchemaLocation location);

  /**
   * Get the current location for the source of input to the parser.
   *
   * @return the location of the input source.
   */
  DataSchemaLocation getLocation();

  /**
   * Return the top level {@link DataSchema}'s.
   *
   * The top level DataSchema's represent the types
   * that are not defined within other types.
   *
   * @return the list of top level {@link DataSchema}'s in the
   *         order that are defined.
   */
  List<DataSchema> topLevelDataSchemas();

  /**
   * Look for {@link DataSchema} with the specified name.
   *
   * @param fullName to lookup.
   * @return the {@link DataSchema} if lookup was successful else return null.
   */
  DataSchema lookupName(String fullName);

  /**
   * Return whether any error occurred during parsing.
   *
   * @return true if at least one error occurred during parsing.
   */
  boolean hasError();

  /**
   * Return the error message from parsing.
   *
   * @return the error message.
   */
  String errorMessage();

  StringBuilder errorMessageBuilder();

  /**
   * Dump the top level schemas.
   *
   * @return a textual dump of the top level schemas.
   */
  String schemasToString();
}
