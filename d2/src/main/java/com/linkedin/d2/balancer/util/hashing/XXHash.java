/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import net.openhft.hashing.LongHashFunction;


/**
 * Faster alternative to MD5 when cryptographic strength is not required.
 * Used the same hash function as in MPConsistencyHashRing
 */
public class XXHash implements HashFunction<String[]> {
  //In order to ensure the consistency of the hashing function,
  // we seed the hash function in the beginning with fixed seed.
  private static final LongHashFunction hashFunction = LongHashFunction.xx_r39(0xDEADBEEF);

  @Override
  public int hash(String[] keyTokens) {
    return Long.hashCode(hashLong(keyTokens));
  }

  @Override
  public long hashLong(String[] keyTokens) {
    StringBuilder concatenatedKeys = new StringBuilder();
    for (int i = 0; i < keyTokens.length; i++) {
      concatenatedKeys.append(keyTokens[i]);
    }
    return hashFunction.hashChars(concatenatedKeys.toString());
  }
}
