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

package com.linkedin.data.transform;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.Escaper;

public class TestEscaper
{

  @Test
  public void testEscaping()
  {
    assertEquals(Escaper.replaceAll("", "$", "$$"), "");
    assertEquals(Escaper.replaceAll("$", "$", "$$"), "$$");
    assertEquals(Escaper.replaceAll(" $", "$", "$$"), " $$");
    assertEquals(Escaper.replaceAll(" $ ", "$", "$$"), " $$ ");
    assertEquals(Escaper.replaceAll(" $$ ", "$", "$$"), " $$$$ ");
    assertEquals(Escaper.replaceAll("$ $ ", "$", "$$"), "$$ $$ ");
    assertEquals(Escaper.replaceAll("$ $ $$", "$", "$$"), "$$ $$ $$$$");
    assertEquals(Escaper.replaceAll("$start", "$", "$$"), "$$start");
    assertEquals(Escaper.replaceAll("$*$", "$", "$$"), "$$*$$");
  }

  @Test
  public void testUnescaping()
  {
    assertEquals(Escaper.replaceAll("", "$$", "$"), "");
    assertEquals(Escaper.replaceAll("$$", "$$", "$"), "$");
    assertEquals(Escaper.replaceAll(" $$", "$$", "$"), " $");
    assertEquals(Escaper.replaceAll(" $$ ", "$$", "$"), " $ ");
    assertEquals(Escaper.replaceAll(" $$$$ ", "$$", "$"), " $$ ");
    assertEquals(Escaper.replaceAll("$$ $$ ", "$$", "$"), "$ $ ");
    assertEquals(Escaper.replaceAll("$$ $$ $$$$", "$$", "$"), "$ $ $$");
    assertEquals(Escaper.replaceAll("$$start", "$$", "$"), "$start");
    assertEquals(Escaper.replaceAll("$$*$$", "$$", "$"), "$*$");

    assertEquals(Escaper.replaceAll(" $$$ ", "$$", "$"), " $$ ");  //first two dollars are unescaped
  }

  @Test
  public void testEscapePathSegment()
  {
    assertEquals(Escaper.escapePathSegment("*"), "*");
    assertEquals(Escaper.escapePathSegment("$*"), "$$*");
    assertEquals(Escaper.escapePathSegment(PathSpec.WILDCARD), "$*");
  }

  @Test
  public void testUnescapePathSegment()
  {
    assertEquals(Escaper.unescapePathSegment("*"), "*");
    assertEquals(Escaper.unescapePathSegment("$$*"), "$*");
    assertTrue(Escaper.unescapePathSegment("$*") == PathSpec.WILDCARD);
    assertFalse(Escaper.unescapePathSegment("*") == PathSpec.WILDCARD);
    assertFalse(Escaper.unescapePathSegment("$$*") == PathSpec.WILDCARD);
  }

}
