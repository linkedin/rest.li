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

package com.linkedin.data.it;


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import java.io.IOException;

public class IteratorTestData
{
  private static final String dataJson = 
      "{\n" +
      "  \"foo\": [\n" +
      "    {\"id\": 1},\n " +
      "    {\"id\": 2},\n" +
      "    {\"id\": 3}\n" +
      "  ],\n" +
      "  \"y\": \"string\", \n" +
      "  \"nested\": {\n" +
      "    \"foo\": [],\n" +
      "    \"nested\": {\n" +
      "      \"foo\": [\n" +
      "        {\"id\": 4},\n" +
      "        {\"id\": 5}\n" +
      "      ]\n" +
      "    }\n" +
      "  }\n" +
      "}\n";

  private static final String schemaJson = 
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"Foo\",\n" +
    "  \"fields\" : [\n" +
    "    { \"name\" : \"foo\", \"type\" : {\n" +
    "        \"type\" : \"array\",\n" +
    "        \"items\" : {\n" +
    "          \"type\": \"record\",\n" +
    "          \"name\": \"Bar\",\n" +
    "          \"fields\": [\n" +
    "            { \"name\": \"id\", \"type\": \"int\" }\n" +
    "          ]\n" +
    "         }\n" +
    "      }\n" +
    "    },\n" +
    "    { \"name\": \"y\", \"type\":\"string\", \"optional\":true },\n" +
    "    { \"name\": \"nested\", \"type\":\"Foo\", \"optional\":true }\n" +
    "  ]\n" +
    "}\n"
  ;

  public static final PathSpec PATH_TO_ID = new PathSpec("foo", PathSpec.WILDCARD, "id");

  public static final Predicate LESS_THAN_3_CONDITION = new Predicate()
  {
    
    @Override
    public boolean evaluate(DataElement element)
    {
      Integer item = (Integer)element.getValue();
      return (item < 3);
    }
  };
  
  public static SimpleTestData createSimpleTestData() throws IOException
  {
    return new SimpleTestData();
  }
  
  public static class SimpleTestData
  {
    private DataMap value;
    private RecordDataSchema schema;
    private SimpleDataElement data;
    
    public SimpleTestData() throws IOException
    {
      value = TestUtil.dataMapFromString(dataJson);
      schema = (RecordDataSchema) TestUtil.dataSchemaFromString(schemaJson);
      data = new SimpleDataElement(value, schema);
    }
    
    public DataMap getValue()
    {
      return value;
    }
    
    public RecordDataSchema getSchema()
    {
      return schema;
    }
    
    public SimpleDataElement getDataElement()
    {
      return data;
    }
  }
}
