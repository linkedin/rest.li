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

package com.linkedin.pegasus.generator.override;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;


/**
 * @author Min Chen
 */
public class TestUri
{
  @Test
  public void testUri()
  {
    UriClient uc = new UriClient();
    URI input = URI.create("http://www.linkedin.com");
    uc.setRequired(input);
    URI output = uc.getRequired();
    Assert.assertEquals(input, output);

    URI outputAgain = uc.getRequired();
    // test caching for custom types with assumption that each time UriCoercer.coerceOutput() will create new Uri object
    Assert.assertSame(outputAgain, output);
  }
}
