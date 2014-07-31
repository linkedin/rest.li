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

/* $Id$ */
package com.linkedin.r2.caprep.db;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for serializing/deserializing messages to/from streams.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface MessageSerializer
{
  /**
   * Serialize a {@link Request} to an {@link OutputStream}.
   *
   * @param out the target for the serialization.
   * @param req the request to be serialized.
   * @throws IOException
   */
  void writeRequest(OutputStream out, Request req) throws IOException;

  /**
   * Serialize a {@link Response} to an {@link OutputStream}.
   *
   * @param out the target for the serialization.
   * @param res the response to be serialized.
   * @throws IOException
   */
  void writeResponse(OutputStream out, Response res) throws IOException;

  /**
   * Read a {@link RestRequest} from an {@link InputStream}.
   *
   * @param in the source for the deserialization.
   * @return a {@link RestRequest} object obtained from the stream.
   * @throws IOException
   */
  RestRequest readRestRequest(InputStream in) throws IOException;

  /**
   * Read a {@link RestResponse} from an {@link InputStream}.
   *
   * @param in the source for the deserialization.
   * @return a {@link RestResponse} object obtained from the stream.
   * @throws IOException
   */
  RestResponse readRestResponse(InputStream in) throws IOException;
}
