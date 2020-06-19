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

package com.linkedin.restli.server.combined;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;

/**
 * @author dellamag
 */
public class CombinedTestDataModels
{
  public static class Foo extends RecordTemplate
  {
    // schema content is irrelevant, this Foo is never used
    private final static DataSchema SCHEMA = DataTemplateUtil.parseSchema("{\"type\":\"record\", \"name\":\"Foo\", \"namespace\":\"com.linkedin.restli.server.combined\", \"fields\" : [{ \"name\":\"booleanField\", \"type\":\"boolean\" }]}");
    public Foo(DataMap map)
    {
      super(map, null);
    }
  }

  public static class FooMetaData extends RecordTemplate
  {
    // schema content is irrelevant, this Foo is never used
    private final static DataSchema SCHEMA = DataTemplateUtil.parseSchema("{\"type\":\"record\", \"name\":\"FooMetadata\", \"namespace\":\"com.linkedin.restli.server.combined\", \"fields\" : [{ \"name\":\"booleanField\", \"type\":\"boolean\" }]}");
    public FooMetaData(DataMap map)
    {
      super(map, null);
    }
  }

  public static class DummyKeyPart extends RecordTemplate
  {
    public DummyKeyPart(DataMap map)
    {
      super(map, null);
    }
  }

  public static class DummyParamsPart extends RecordTemplate
  {
    // schema content is irrelevant, DummyParamsPart is never used
    private final static DataSchema SCHEMA = DataTemplateUtil.parseSchema("{\"type\":\"record\", \"name\":\"DummyParamsPart\", \"namespace\":\"com.linkedin.restli.server.combined\", \"fields\" : [{ \"name\":\"booleanField\", \"type\":\"boolean\" }]}");
    public DummyParamsPart(DataMap map)
    {
      super(map, null);
    }
  }
}
