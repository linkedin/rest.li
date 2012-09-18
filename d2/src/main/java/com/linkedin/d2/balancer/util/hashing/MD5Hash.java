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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.util.hashing;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MD5Hash implements HashFunction<String[]>
{
  private static final Charset UTF8 = Charset.forName("UTF8");
  private static final byte[] ONE_NULL = new byte[] { 0x00 };

  public int hash(String[] keyTokens)
  {
    byte[] digest = getMD5Digest(keyTokens);
    return ((0xff & digest[12]) << 24) | ((0xff & digest[13]) << 16) |
        ((0xff & digest[14]) << 8) | (0xff & digest[15]);
  }

  public long hashLong(String [] keyTokens)
  {
    byte[] digest = getMD5Digest(keyTokens);
    return ((0xffL & digest[8]) << 56) | ((0xffL & digest[9]) << 48) |
        ((0xffL & digest[10]) << 40) | ((0xffL & digest[11]) << 32) |
        ((0xffL & digest[12]) << 24) | ((0xffL & digest[13]) << 16) |
        ((0xffL & digest[14]) << 8) | (0xffL & digest[15]);
  }

  private byte[] getMD5Digest(String [] keyTokens)
  {
    try
    {
      MessageDigest md = MessageDigest.getInstance("MD5");
      for (int i = 0; i < keyTokens.length; i++)
      {
        md.update(keyTokens[i].getBytes(UTF8));
        // Boundary between fields; 0x00 byte does not occur in UTF8 strings
        md.update(ONE_NULL);
      }
      return md.digest();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new IllegalStateException(e);
    }
  }
}
