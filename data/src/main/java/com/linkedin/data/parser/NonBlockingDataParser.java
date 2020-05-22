/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data.parser;

import com.linkedin.data.ByteString;
import java.io.IOException;


/**
 * Data parser interface invoked by non blocking decoder.
 *
 * This interface contains methods that are invoked when parsing a Data object.
 * Each method represents a different kind of event/read action
 *
 * Methods can throw IOException as a checked exception to indicate parsing error.
 *
 * @author amgupta1
 */
public interface NonBlockingDataParser
{
  /**
   * Internal tokens, used to identify types of elements in data during decoding
   */
  enum Token
  {
    /**
     * START_OBJECT is returned when encountering signals starting of an Object/map value.
     */
    START_OBJECT,
    /**
     * END_OBJECT is returned when encountering signals ending of an Object/map value
     */
    END_OBJECT,
    /**
     * START_ARRAY is returned when encountering signals starting of an Array value
     */
    START_ARRAY,
    /**
     * END_ARRAY is returned when encountering signals ending of an Array value
     */
    END_ARRAY,
    /**
     * STRING is returned when encountering a string value, field name or reference
     */
    STRING,
    /**
     * RAW_BYTES is returned when encountering chunk of raw bytes
     */
    RAW_BYTES,
    /**
     * INTEGER is returned when encountering integer value
     */
    INTEGER,
    /**
     * LONG is returned when encountering long value
     */
    LONG,
    /**
     * FLOAT is returned when encountering float decimal value
     */
    FLOAT,
    /**
     * DOUBLE is returned when encountering double decimal value
     */
    DOUBLE,
    /**
     * BOOL_TRUE is returned when encountering boolean true value
     */
    BOOL_TRUE,
    /**
     * BOOL_FALSE is returned when encountering boolean false value
     */
    BOOL_FALSE,
    /**
     * NULL is returned when encountering "null" in value context
     */
    NULL,
    /**
     * NOT_AVAILABLE can be returned if {@link NonBlockingDataParser} implementation can not currently
     * return the requested token (usually next one), but that may be able to determine this in future.
     * non-blocking parsers can not block to wait for more data to parse and must return something.
     */
    NOT_AVAILABLE,
    /**
     * Token returned at point when all feed input has been exhausted or
     * input feeder has indicated no more input will be forthcoming.
     */
    EOF_INPUT
  }

  /**
   * Method that can be called to feed more data if {@link #nextToken()} returns {@link Token#NOT_AVAILABLE}
   *
   * @param data Byte array that contains data to feed: caller must ensure data remains
   *    stable until it is fully processed
   * @param offset Offset where input data to process starts
   * @param len length of bytes to be feed from the input array
   *
   * @throws IOException if the state is such that this method should not be called
   *   (has not yet consumed existing input data, or has been marked as closed)
   */
  void feedInput(byte[] data, int offset, int len) throws IOException;

  /**
   * Method that should be called after last chunk of data to parse has been fed
   * (with {@link #feedInput(byte[], int, int)}). After calling this method,
   * no more data can be fed; and parser assumes no more data will be available.
   */
  void endOfInput();

  /**
   * Main iteration method, which will advance input enough to determine type of the next token, if any.
   * If none remaining (input has no content other than possible white space before ending),
   * {@link Token#EOF_INPUT} will be returned.
   *
   * @return Next token from the input, if any found, or {@link Token#EOF_INPUT} to indicate end-of-input
   */
  Token nextToken() throws IOException;

  /**
   * Method that can be called to get the size of current token Object returned
   * from {@link NonBlockingDataParser#nextToken()}. Ex. {@link Token#START_OBJECT}s it will return size of map;
   * if size is not available returns -1.
   */
  default int getComplexObjSize()
  {
    return -1;
  }

  /**
   * Method for accessing textual representation of the current token;
   * that can be called when the current token is of type {@link Token#STRING}.
   */
  String getString() throws IOException;

  /**
   * Method for accessing raw bytes as {@link ByteString} that can be called when the current
   * token is of type {@link Token#RAW_BYTES}.
   */
  ByteString getRawBytes() throws IOException;

  /**
   * Numeric accessor that can be called when the current token is of type {@link Token#INTEGER} and
   * it can be expressed as a value of Java int primitive type.
   */
  int getIntValue() throws IOException;

  /**
   * Numeric accessor that can be called when the current token is of type {@link Token#LONG} and
   * it can be expressed as a Java long primitive type.
   */
  long getLongValue() throws IOException;

  /**
   * Numeric accessor that can be called when the current token is of type {@link Token#FLOAT} and
   * it can be expressed as a Java float primitive type.
   */
  float getFloatValue() throws IOException;

  /**
   * Numeric accessor that can be called when the current token is of type {@link Token#DOUBLE} and
   * it can be expressed as a Java double primitive type.
   */
  double getDoubleValue() throws IOException;
}
