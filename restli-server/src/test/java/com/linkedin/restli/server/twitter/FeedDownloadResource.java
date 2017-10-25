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

package com.linkedin.restli.server.twitter;


import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceTemplate;

import java.io.IOException;


/**
 * Resource that serve feed downloads.
 */
@RestLiCollection(name="feedDownloads", keyName = "feedId")
public class FeedDownloadResource extends UnstructuredDataCollectionResourceTemplate<Long>
{
  public static final byte[] CONTENT = "hello".getBytes();
  public static final String CONTENT_TYPE = "text/plain";

  @Override
  public void get(Long key, @UnstructuredDataWriterParam UnstructuredDataWriter writer)
  {
    try
    {
      writer.setContentType(CONTENT_TYPE);
      writer.getOutputStream().write(CONTENT);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
}
