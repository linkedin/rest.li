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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * DbSource which obtains messages from a directory on the filesystem.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class DirectoryDbSource implements DbSource
{
  private static final Logger _log = LoggerFactory.getLogger(DirectoryDbSource.class);

  private final Map<Request, Response> _db;
  private final MessageSerializer _serializer;

  /**
   * Construct a new instance with a specified directory path and serializer.
   *
   * @param dir the directory path to be used as a message store.
   * @param serializer the {@link MessageSerializer} to use for deserializing messages.
   * @throws IOException
   */
  public DirectoryDbSource(String dir, MessageSerializer serializer) throws IOException
  {
    this(new File(dir), serializer);
  }

  /**
   * Construct a new instance with the specified directory and serializer.
   *
   * @param dir the {@link File} object for the directory to be used as a message store.
   * @param serializer the {@link MessageSerializer} to use for deserializing messages.
   * @throws IOException
   */
  public DirectoryDbSource(File dir, MessageSerializer serializer) throws IOException
  {
    _db = loadDb(dir, serializer);
    _serializer = serializer;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Response> T replay(Request req)
  {
    try
    {
      return (T)_db.get(canonicalize(req));
    }
    catch (Exception e)
    {
      _log.debug("Failed to canonicalize request: " + req, e);
      return null;
    }
  }

  private Map<Request, Response> loadDb(File dir, MessageSerializer serializer) throws IOException
  {
    final Map<Request, Response> db = new HashMap<Request, Response>();

    final String[] ids = DirectoryDbUtil.listRequestIds(dir);
    Arrays.sort(ids);

    for (String id : ids)
    {
      InputStream reqIn = null;
      InputStream resIn = null;
      try
      {
        reqIn = new FileInputStream(DirectoryDbUtil.requestFileName(dir, id));
        final Request req = serializer.readRestRequest(reqIn);

        resIn = new FileInputStream(DirectoryDbUtil.responseFileName(dir, id));
        final Response res = serializer.readRestResponse(resIn);

        db.put(canonicalize(req), res);
      }
      catch (IOException e)
      {
        _log.warn("Failed to parse request or response for: " + id, e);
      }
      finally
      {
        closeSilently(reqIn);
        closeSilently(resIn);
      }
    }

    return db;
  }

  private Request canonicalize(Request req)
  {
    return req.requestBuilder().buildCanonical();
  }

  private void closeSilently(Closeable closeable)
  {
    if (closeable != null)
    {
      try
      {
        closeable.close();
      }
      catch (IOException e)
      {
        // Ignore error
      }
    }
  }
}
