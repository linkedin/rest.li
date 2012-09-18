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

package com.linkedin.data.transform.filter;

import static com.linkedin.data.TestUtil.dataMapFromString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.transform.DataMapProcessor;
import com.linkedin.data.transform.DataProcessingException;

public class TestFilter
{

  @Test
  public void testPathIncludedInError() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': { 'x': 'a'}}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { 'x': { 'y': 1}}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(true);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 1, "expected exactly 1 error");
      Message m = e.getMessages().get(0);
      assertNotNull(m.getPath(), "path should be set on a message");
      assertEquals(m.getPath(), new Object[] {"a", "x"});
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testFastFailError() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': { 'x': 'a'}, 'b': 'b'}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { 'x': { 'y': 1}}, 'b': { 'z': 1}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(true);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 1, "expected exactly 1 error in fast fail mode");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testNonFastFailError() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': { 'x': 'a'}, 'b': 'b'}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { 'x': { 'y': 1}}, 'b': { 'z': 1}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(false);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 2, "expected exactly 2 errors in non fast fail mode");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testErrorMessagesForArraysNotFastFail() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': [1, 2, 3, 4, 5]}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { '$*': { 'y': 1}}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(false);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 5, "expected exactly 5 errors in non fast fail mode");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testErrorMessagesForArraysFastFail() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': [1, 2, 3, 4, 5]}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { '$*': { 'y': 1}}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(true);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 1, "expected exactly 1 error in non fast fail mode");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testIncorrectFilter() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': [1, 2, 3, 4, 5]}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { '$*': 'hola'}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(false);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 1, "expected exactly 1 error in non fast fail mode");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

  @Test
  public void testMaskCompositionDuringFilteringDoesNotOverwriteOriginalMask() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': { 'b': 'b'}}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ '$*': { 'c': { 'd': 0}}, 'a': 1}".replace('\'', '"'));
    String originalFilter = filter.toString();
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    processor.run(false);
    assertEquals(filter.toString(), originalFilter, "filter should not be modified");
  }

  @Test
  public void testInvalidArrayRangeInFilter() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap data = dataMapFromString("{ 'a': [1, 2, 3, 4, 5]}".replace('\'', '"'));
    DataMap filter = dataMapFromString("{ 'a': { '$start': -2, '$count': -1}}".replace('\'', '"'));
    DataMapProcessor processor = new DataMapProcessor(new Filter(), filter, data);
    boolean thrown = false;
    try {
      processor.run(false);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 2);
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }



}
