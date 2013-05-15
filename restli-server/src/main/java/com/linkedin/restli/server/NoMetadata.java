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

package com.linkedin.restli.server;


import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;


/**
 * This is marker class, and not meant to be instantiated. Use as the metadata type in CollectResult to indicate that no metadata is provided.
 * See example in the integration test
 *
 * @author Keren Jin
 */
public final class NoMetadata extends RecordTemplate
{
  private final static RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"NoMetadata\",\"namespace\":\"com.linkedin.restli.server\",\"doc\":\"This is marker class, and not meant to be instantiated. Use as the metadata type in CollectResult to indicate that no metadata is provided.\",\"fields\":[]}");

  private NoMetadata()
  {
    super(null, null);
  }
}
