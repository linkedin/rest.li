/*
 Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.r2.caprep.db;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Writes to the file locations specified on construction.
 * 
 * @author jbetz@linkedin.com
 */
public class FixedFileDbSink implements DbSink
{
  private final File _requestFile;
  private final File _responseFile;
  private final MessageSerializer _serializer;

  public FixedFileDbSink(File requestFile, File responseFile, MessageSerializer serializer) throws IOException
  {
    _requestFile = requestFile;
    _responseFile = responseFile;
    _serializer = serializer;
  }

  @Override
  public void record(RestRequest req, RestResponse res) throws IOException
  {
    writeRequest(req, _requestFile);
    writeResponse(res, _responseFile);
  }

  private void writeRequest(RestRequest req, File file) throws IOException
  {
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

  private void writeResponse(RestResponse res, File file) throws IOException
  {
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
