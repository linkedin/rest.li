/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.RestLiServiceException;

import java.io.IOException;


/**
 * Shared logic and constants for greeting unstructured data resources.
 */
public final class GreetingUnstructuredDataUtils
{
  public static byte[] UNSTRUCTURED_DATA_BYTES = "hello".getBytes();
  public static String MIME_TYPE = "text/csv";
  public static String CONTENT_DISPOSITION_VALUE = "inline";

  /**
   * Returns a GOOD unstructured data repsonse with all headers properly set
   */
  public static void respondGoodUnstructuredData(UnstructuredDataWriter writer)
  {
    writer.setContentType(MIME_TYPE);
    try
    {
      writer.getOutputStream().write(UNSTRUCTURED_DATA_BYTES);
    }
    catch (IOException e)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "failed to write unstructured data", e);
    }
  }

  /**
   * Returns a BAD unstructured data repsonse with missing headers
   */
  public static void respondBadUnstructuredData(UnstructuredDataWriter writer)
  {
    try
    {
      writer.getOutputStream().write(UNSTRUCTURED_DATA_BYTES);
    }
    catch (IOException e)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "failed to write unstructured data", e);
    }
  }
}
