/*
   Copyright (c) 2015 LinkedIn Corp.

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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * LinkedIn elects to include this software in this distribution under the Apache License.
 *
 * Modifications:
 *   Repackaged original source under com.linkedin.r2.util package.
 *   Renamed class from IOUtils to IOUtil.
 *   Only retained methods needed for toString(InputStream input, String encoding) and toByteArray(InputStream input).
 *   Removed dependency on org.apache.commons.io.output.ByteArrayOutputStream.
 */

package com.linkedin.r2.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;


/**
 * Utility class for converting {@link InputStream} to byte array or string.
 * Copied methods from apache commons-io IOUtils class (version 1.4).
 *
 * @author Soojung Ha
 */
public class IOUtil
{
  /**
   * The default buffer size to use.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(Reader input, Writer output) throws IOException
  {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer)))
    {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(InputStream input, OutputStream output)
      throws IOException
  {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer)))
    {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p/>
   * Large streams (over 2GB) will return a bytes copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of bytes cannot be returned as an int. For large streams
   * use the <code>copyLarge(InputStream, OutputStream)</code> method.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @throws ArithmeticException  if the byte count is too large
   * @since Commons IO 1.1
   */
  public static int copy(InputStream input, OutputStream output) throws IOException
  {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE)
    {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a
   * <code>Writer</code> using the default character encoding of the platform.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p/>
   * This method uses {@link InputStreamReader}.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output)
      throws IOException
  {
    InputStreamReader in = new InputStreamReader(input);
    copy(in, output);
  }

  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p/>
   * Large streams (over 2GB) will return a chars copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of chars cannot be returned as an int. For large streams
   * use the <code>copyLarge(Reader, Writer)</code> method.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @throws ArithmeticException  if the character count is too large
   * @since Commons IO 1.1
   */
  public static int copy(Reader input, Writer output) throws IOException
  {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE)
    {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a
   * <code>Writer</code> using the specified character encoding.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p/>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p/>
   * This method uses {@link InputStreamReader}.
   *
   * @param input    the <code>InputStream</code> to read from
   * @param output   the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output, String encoding)
      throws IOException
  {
    if (encoding == null)
    {
      copy(input, output);
    }
    else
    {
      InputStreamReader in = new InputStreamReader(input, encoding);
      copy(in, output);
    }
  }

  /**
   * Get the contents of an <code>InputStream</code> as a String
   * using the specified character encoding.
   * <p/>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input    the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   */
  public static String toString(InputStream input, String encoding)
      throws IOException
  {
    StringWriter sw = new StringWriter();
    copy(input, sw, encoding);
    return sw.toString();
  }

  /**
   * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
   * <p/>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   */
  public static byte[] toByteArray(InputStream input) throws IOException
  {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output);
    return output.toByteArray();
  }
}
