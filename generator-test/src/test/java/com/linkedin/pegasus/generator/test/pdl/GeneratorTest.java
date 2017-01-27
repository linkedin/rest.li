/*
 Copyright 2015 Coursera Inc.

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

package com.linkedin.pegasus.generator.test.pdl;

import java.io.File;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.PrettyPrinterJacksonDataTemplateCodec;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import static org.testng.Assert.*;


abstract class GeneratorTest
{
  public void assertJson(DataTemplate<DataMap> left, String right)
      throws IOException
  {
    // Types of primitive values in a DataTemplates's DataMap match the corresponding schema field type and may be
    // narrower than the types in a DataMap read directly from JSON. So we round trip all DataMaps from DataTemplates
    // through raw JSON to normalize all types before performing a JSON equality check.
    DataMap leftMap = readJsonToMap(mapToJson(left.data()));
    DataMap rightMap = readJsonToMap(right);
    assertEquals(leftMap, rightMap);
  }

  public DataMap roundTrip(DataMap complex)
      throws IOException
  {
    return readJsonToMap(mapToJson(complex));
  }

  private static final File jsonPath = new File(System.getProperty("testDir") + "/json");

  protected String load(String filename)
  {
    try
    {
      return FileUtils.readFileToString(new File(jsonPath, filename));
    } catch (IOException e)
    {
      fail("Failed to load file: " + filename + ": " + e.getMessage());
      return null;
    }
  }

  private PrettyPrinterJacksonDataTemplateCodec prettyPrinter = new PrettyPrinterJacksonDataTemplateCodec();
  private JacksonDataCodec dataCodec = new JacksonDataCodec();

  private String mapToJson(DataMap dataMap)
      throws IOException
  {
    return prettyPrinter.mapToString(dataMap);
  }

  public DataMap readJsonToMap(String string)
      throws IOException
  {
    return dataCodec.stringToMap(string);
  }
}
