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

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * @author Boyang Chen
 */
public class CookieUtil
{
  /**
   * Encode the cookie string on client side, to have the Header: "Cookie: name=value"
   * @param cookies
   * @return list of cookie strings
   */
  public static List<String> encodeCookies(List<HttpCookie> cookies)
  {
    List<String> cookieStrs = new ArrayList<>();
    if (cookies != null)
    {
      for (HttpCookie cookie : cookies)
      {
        if (cookie != null)
          cookieStrs.add(encodeCookie(cookie));
      }
    }
    return cookieStrs;
  }

  /**
   * Decode cookie string list on client side, of the header starting with "Set-Cookie"
   * @param cookieStrs
   * @return list of cookie objects
   */
  public static List<HttpCookie> decodeSetCookies(List<String> cookieStrs)
  {
    List<HttpCookie> cookies = new ArrayList<>();
    if (cookieStrs != null)
    {
      for (String cookieStr : cookieStrs)
      {
        if (cookieStr == null)
        {
          continue;
        }
        List<HttpCookie> decodedCookie = HttpCookie.parse(cookieStr);
        if (decodedCookie != null)
        {
          for (HttpCookie cookie : decodedCookie)
          {
            cookies.add(cookie);
          }
        }
      }
    }
    return cookies;
  }

  /**
   * Encode Cookie list on server side
   * @param cookies
   * @return list of cookie strings
   */
  public static List<String> encodeSetCookies(List<HttpCookie> cookies)
  {
    List<String> cookieStrs = new ArrayList<>();
    if (cookies != null)
    {
      for (HttpCookie cookie : cookies)
      {
        if (cookie != null)
          cookieStrs.add(encodeSetCookie(cookie));
      }
    }
    return cookieStrs;
  }

  /**
   * Cutomized cookie decoding function, for unknown client communication with potentially multiple cookies in one cookie string.
   * @param cookieStrs
   * @return list of cookie objects
   */
  public static List<HttpCookie> decodeCookies(List<String> cookieStrs)
  {
    List<HttpCookie> cookies = new ArrayList<>();
    if (cookieStrs == null)
    {
      return cookies;
    }

    for (String cookieStr : cookieStrs)
    {
      if (cookieStr == null)
      {
        continue;
      }
      StringTokenizer tokenizer = new StringTokenizer(cookieStr, ";");
      String nameValuePair;

      HttpCookie cookieToBeAdd = null;
      while (tokenizer.hasMoreTokens())
      {
        nameValuePair = tokenizer.nextToken();

        int index = nameValuePair.indexOf('=');
        if (index != -1)
        {
          String name = nameValuePair.substring(0, index).trim();
          String value = stripOffSurrounding(nameValuePair.substring(index + 1).trim());
          if (name.charAt(0) != '$')
          {
            if (cookieToBeAdd != null)
            {
              cookies.add(cookieToBeAdd);
            }
            cookieToBeAdd = new HttpCookie(name, value);
          }
          else if (cookieToBeAdd != null)
          {
            if (name.equals("$Path"))
            {
              cookieToBeAdd.setPath(value);
            }
            else if (name.equals("$Domain"))
            {
              cookieToBeAdd.setDomain(value);
            }
            else if (name.equals("$Port"))
            {
              cookieToBeAdd.setPortlist(value);
            }
          }
        }
        else
        {
          throw new IllegalArgumentException("Invalid cookie name-value pair");
        }
      }
      if (cookieToBeAdd != null)
      {
        cookies.add(cookieToBeAdd);
      }
    }
    return cookies;
  }

  /**
   * Encode single cookie on client Side
   * @param cookie
   * @return cookie string
   */
  public static String encodeCookie(HttpCookie cookie)
  {
    if (cookie == null)
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    sb.append(cookie.getName()).append("=").append(cookie.getValue());
    return sb.toString();
  }

  public static String encodeSetCookie(HttpCookie cookie)
  {
    if (cookie == null)
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    sb.append(cookie.getName()).append("=").append(cookie.getValue());

    if (cookie.getPath() != null)
    {
      sb.append(";Path=").append(cookie.getPath());
    }
    if (cookie.getDomain() != null)
    {
      sb.append(";Domain=").append(cookie.getDomain());
    }
    if (cookie.getPortlist() != null)
    {
      // Port value should be quoted according to RFC 2965 Section 3.2.2.
      sb.append(";Port=\"").append(cookie.getPortlist()).append('"');
    }

    sb.append(";Max-Age=").append(Long.toString(cookie.getMaxAge()));
    sb.append(";Version=").append(Integer.toString(cookie.getVersion()));
    if (cookie.getDiscard())
    {
      sb.append(";Discard");
    }
    if (cookie.getSecure())
    {
      sb.append(";Secure");
    }
    if (cookie.isHttpOnly())
    {
      sb.append(";HttpOnly");
    }

    if (cookie.getComment() != null)
    {
      sb.append(";Comment=").append(cookie.getComment());
    }
    if (cookie.getCommentURL() != null)
    {
      // CommentURL value should be quoted according to RFC 2965 Section 3.2.2.
      sb.append(";CommentURL=\"").append(cookie.getCommentURL()).append('"');
    }

    return sb.toString();
  }

  /**
   * hepler function for extracting only the raw string
   * @param s
   * @return
   */
  private static String stripOffSurrounding(String s)
  {
    if (s != null && s.length() > 2)
    {
      if ((s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') || (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\''))
      {
        return s.substring(1, s.length() - 1);
      }
    }
    return s;
  }
}
