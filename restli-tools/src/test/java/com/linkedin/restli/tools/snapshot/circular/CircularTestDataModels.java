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

package com.linkedin.restli.tools.snapshot.circular;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class CircularTestDataModels
{

  public static class A extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
      "{\"type\" : \"record\", \"name\" : \"A\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"b\", \"type\" : {\"type\" : \"record\", \"name\" : \"B\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"a\", \"type\" : \"A\" } ] } } ] }"));

    public A()
    {
      super(new DataMap(), SCHEMA);
    }

    public A(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class B extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
      "{\"type\" : \"record\", \"name\" : \"B\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"a\", \"type\" : {\"type\" : \"record\", \"name\" : \"A\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"b\", \"type\" : \"B\" } ] } } ] }"));

    public B()
    {
      super(new DataMap(), SCHEMA);
    }

    public B(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class C extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\" : \"record\", \"name\" : \"C\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"a\", \"type\" : {\"type\" : \"record\", \"name\" : \"A\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"b\", \"type\" : {\"type\" : \"record\", \"name\" : \"B\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"a\", \"type\" : \"A\" } ] } } ] } } ] }"));

    public C()
    {
      super(new DataMap(), SCHEMA);
    }

    public C(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class D extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\" : \"record\", \"name\" : \"D\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"b\", \"type\" : {\"type\" : \"record\", \"name\" : \"B\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"a\", \"type\" : {\"type\" : \"record\", \"name\" : \"A\", \"namespace\" : \"com.linkedin.restli.tools.snapshot.circular\", \"fields\" : [ { \"name\" : \"b\", \"type\" : \"B\" } ] } } ] } } ] }"));

    public D()
    {
      super(new DataMap(), SCHEMA);
    }

    public D(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

}
