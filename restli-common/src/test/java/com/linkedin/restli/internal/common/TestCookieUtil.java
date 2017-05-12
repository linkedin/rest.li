/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.common;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Boyang Chen
 */
public class TestCookieUtil
{
  private HttpCookie cookieA;
  private HttpCookie cookieB;
  private HttpCookie cookieC;
  private HttpCookie cookieD;

  @BeforeClass
  public void setUp() throws URISyntaxException
  {
    cookieA = new HttpCookie("a", "android");
    cookieA.setDomain(".android.com");
    cookieA.setPath("/source/");
    cookieA.setDiscard(false);
    cookieA.setMaxAge(125L);
    cookieA.setSecure(true);
    cookieA.setHttpOnly(true);

    cookieB = new HttpCookie("b", "boss");
    cookieC = new HttpCookie("c", "ios");
    cookieD = new HttpCookie("d", "desk");
  }

  @AfterClass
  public void tearDown()
  {
    cookieA = null;
    cookieC = null;
    cookieD = null;
  }

  @Test
  public void testSimpleCookieFromServer()
  {
    cookieA.setComment("nothing important");
    List<String> encodeStrs = CookieUtil.encodeSetCookies(Collections.singletonList(cookieA));
    List<HttpCookie> cookieList = CookieUtil.decodeSetCookies(encodeStrs);

    Assert.assertEquals(1, cookieList.size());

    HttpCookie decodedCookie = cookieList.get(0);
    Assert.assertEquals(decodedCookie.getName(), cookieA.getName());
    Assert.assertEquals(decodedCookie.getValue(), cookieA.getValue());
    Assert.assertEquals(decodedCookie.getDomain(), cookieA.getDomain());
    Assert.assertEquals(decodedCookie.getPath(), cookieA.getPath());
    Assert.assertEquals(decodedCookie.getDiscard(), cookieA.getDiscard());
    Assert.assertEquals(decodedCookie.getMaxAge(), cookieA.getMaxAge());
    Assert.assertEquals(decodedCookie.getSecure(), cookieA.getSecure());
    Assert.assertEquals(decodedCookie.isHttpOnly(), cookieA.isHttpOnly());
  }

  @Test
  public void testCookieAttributeEncoding()
  {
    String encodedCookie = CookieUtil.encodeSetCookie(cookieA);

    Assert.assertTrue(encodedCookie.contains("Domain=.android.com"));
    Assert.assertTrue(encodedCookie.contains("Path=/source/"));
    Assert.assertTrue(encodedCookie.contains("Max-Age=125"));
    Assert.assertTrue(encodedCookie.contains("HttpOnly"));
  }

  @Test
  public void testSimpleCookieFromClient()
  {
    cookieA.setComment("nothing important");
    List<String> encodeStrs = CookieUtil.encodeCookies(Collections.singletonList(cookieA));
    List<HttpCookie> cookieList = CookieUtil.decodeCookies(encodeStrs);

    Assert.assertEquals(cookieA.getName(), cookieList.get(0).getName());
    Assert.assertEquals(cookieA.getValue(), cookieList.get(0).getValue());
  }

  @Test
  public void testInvalidCookieFromClient()
  {
    cookieA.setComment("nothing important");
    List<String> encodeStrs = Collections.singletonList("$Domain=.linkedin.com; $Port=80;  $Path=/; $Version=0;");
    List<HttpCookie> cookieList = CookieUtil.decodeCookies(encodeStrs);

    Assert.assertEquals(0, cookieList.size());
  }

  @Test
  public void testEvilComment() throws URISyntaxException
  {
    cookieA.setComment("http://google.com/;ses=source/");
    List<HttpCookie> cookieList = CookieUtil.decodeSetCookies(CookieUtil.encodeSetCookies(Collections.singletonList(
        cookieA)));
    Assert.assertNotEquals(cookieA.getComment(), cookieList.get(0).getComment());
  }

  @Test
  public void testDecodeMultipleCookiesSingleLine() throws URISyntaxException
  {

    cookieC.setPath("/source/");
    String combinedHeader = CookieUtil.encodeCookies(Collections.singletonList(cookieA)).get(0) + ";" + CookieUtil.encodeCookies(
        Collections.singletonList(cookieC)).get(0) + ";" +  CookieUtil.encodeCookies(Collections.singletonList(
        cookieD)).get(0);

    List<HttpCookie> cookieList = CookieUtil.decodeCookies(Collections.singletonList(combinedHeader));
    Assert.assertEquals(cookieList.size(), 3);
  }

  @Test
  public void testDecodeSingleCookieWithAttr()
  {
    String cookieStr = cookieA.toString();
    Assert.assertEquals(CookieUtil.decodeCookies(Collections.singletonList(cookieStr)), Collections.singletonList(cookieA));
  }

  @Test
  public void testDecodeMultipleCookiewWithAttr()
  {
    String combinedHeader = cookieA.toString() + ";" + cookieC.toString() + ";" + cookieD.toString();
    List<HttpCookie> cookies = Arrays.asList(cookieA, cookieC, cookieD);
    Assert.assertEquals(CookieUtil.decodeCookies(Collections.singletonList(combinedHeader)), cookies);
  }

  @Test
  public void testDifferentCookieStringsCombination()
  {
    List<HttpCookie> cookies = Arrays.asList(cookieC, cookieD, cookieA, cookieB);
    String combinedHeader = cookieC.toString() + ";" + cookieD.toString() + ";" + cookieA.toString();
    String cookieBStr = cookieB.toString();
    Assert.assertEquals(CookieUtil.decodeCookies(Arrays.asList(combinedHeader, cookieBStr)), cookies);
  }
}
