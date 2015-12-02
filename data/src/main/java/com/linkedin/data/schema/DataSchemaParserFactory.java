package com.linkedin.data.schema;


public interface DataSchemaParserFactory
{

  /**
   * Create a new parser that will use the specified resolver.
   *
   * @param resolver to be provided to the parser.
   * @return a new parser.
   */
  PegasusSchemaParser create(DataSchemaResolver resolver);
}