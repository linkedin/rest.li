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

package com.linkedin.restli.server;


import com.linkedin.data.ByteString;
import com.linkedin.java.util.concurrent.Flow;


/**
 * A wrapper of {@link com.linkedin.java.util.concurrent.Flow.Publisher} and ContentType, represents
 * an result of unstructured data that supports reactive streaming.
 */
public class UnstructuredDataReactiveResult
{
  private final Flow.Publisher<ByteString> _publisher;
  private String _contentType;

  public UnstructuredDataReactiveResult(Flow.Publisher<ByteString> publisher, String contentType)
  {
    _publisher = publisher;
    _contentType = contentType;
  }

  public Flow.Publisher<ByteString> getPublisher()
  {
    return _publisher;
  }

  public String getContentType()
  {
    return _contentType;
  }
}
