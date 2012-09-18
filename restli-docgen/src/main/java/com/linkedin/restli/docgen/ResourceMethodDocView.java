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

package com.linkedin.restli.docgen;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.RestMethodSchema;

/**
 * Aggregation of data passed to HTML template pages.
 *
 * @author dellamag
 */
public class ResourceMethodDocView
{
  /**
   * @param methodSchema {@link RestMethodSchema}, {@link FinderSchema} or {@link ActionSchema}
   * @param capture example data of request and response
   * @param doc documentation of the method
   * @param prettyPrintRequestEntity request entity data in pretty printed format
   * @param prettyPrintResponseEntity response entity data in pretty printed format
   */
  public ResourceMethodDocView(RecordTemplate methodSchema,
                               RequestResponsePair capture,
                               String doc,
                               String prettyPrintRequestEntity,
                               String prettyPrintResponseEntity)
  {
    _methodSchema = methodSchema;
    _capture = capture;
    _doc = doc;
    _prettyPrintRequestEntity = prettyPrintRequestEntity;
    _prettyPrintResponseEntity = prettyPrintResponseEntity;
  }

  /**
   * @return Union of {@link RestMethodSchema}, {@link FinderSchema} and {@link ActionSchema}
   */
  public RecordTemplate getMethodSchema()
  {
    return _methodSchema;
  }

  /**
   * @return method schema converted to {@link RestMethodSchema}
   */
  public RestMethodSchema getRestMethodSchema()
  {
    return (RestMethodSchema) _methodSchema;
  }

  /**
   * @return method schema converted to {@link FinderSchema}
   */
  public FinderSchema getFinderSchema()
  {
    return (FinderSchema) _methodSchema;
  }

  /**
   * @return method schema converted to {@link ActionSchema}
   */
  public ActionSchema getActionSchema()
  {
    return (ActionSchema) _methodSchema;
  }

  /**
   * @return example data of request and response
   */
  public RequestResponsePair getCapture()
  {
    return _capture;
  }

  /**
   * @return documentation of the method
   */
  public String getDoc()
  {
    return _doc;
  }

  /**
   * @return request entity data in pretty printed format
   */
  public String getPrettyPrintRequestEntity()
  {
    return _prettyPrintRequestEntity;
  }

  /**
   * @return response entity data in pretty printed format
   */
  public String getPrettyPrintResponseEntity()
  {
    return _prettyPrintResponseEntity;
  }

  private final RecordTemplate _methodSchema;
  private final RequestResponsePair _capture;
  private final String _doc;
  private final String _prettyPrintRequestEntity;
  private final String _prettyPrintResponseEntity;
}
