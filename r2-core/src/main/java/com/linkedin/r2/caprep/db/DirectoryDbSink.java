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

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DbSink which stores messages as files in a directory on the filesystem.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class DirectoryDbSink implements DbSink
{
  private final File _dir;
  private final MessageSerializer _serializer;
  private final AtomicInteger _count;

  /**
   * Construct a new instance with the specified directory path and serializer.
   *
   * @param dir the directory path to be used as a message store.
   * @param serializer the {@link MessageSerializer} to use for serializing messages.
   * @throws IOException
   */
  public DirectoryDbSink(String dir, MessageSerializer serializer) throws IOException
  {
    this(new File(dir), serializer);
  }

  /**
   * Construct a new instance with the specified directory and serializer.
   *
   * @param dir the {@link File} object for the directory to be used as a message store.
   * @param serializer the {@link MessageSerializer} to use for serializing messages.
   * @throws IOException
   */
  public DirectoryDbSink(File dir, MessageSerializer serializer) throws IOException
  {
    _dir = dir;
    if (!_dir.exists() & !_dir.mkdirs())
    {
      throw new IOException("Could not create directory: " + _dir);
    }

    _serializer = serializer;

    int maxCount = -1;
    for (String id : DirectoryDbUtil.listRequestIds(_dir))
    {
      maxCount = Math.max(maxCount, DirectoryDbUtil.getIndex(id));
    }
    _count = new AtomicInteger(maxCount + 1);
  }

  @Override
  public void record(RestRequest req, RestResponse res) throws IOException
  {
    final int id = _count.getAndIncrement();
    writeRequest(req, id);
    writeResponse(res, id);
  }

  private void writeRequest(RestRequest req, int id) throws IOException
  {
    final File file = DirectoryDbUtil.restRequestFileName(_dir, id);
    final FileOutputStream out = new FileOutputStream(file);
    try
    {
      _serializer.writeRequest(out, req);
    }
    finally
    {
      out.close();
    }
  }

  private void writeResponse(RestResponse res, int id) throws IOException
  {
    final File file = DirectoryDbUtil.restResponseFileName(_dir, id);
    final FileOutputStream out = new FileOutputStream(file);
    try
    {
      _serializer.writeResponse(out, res);
    }
    finally
    {
      out.close();
    }
  }
}
