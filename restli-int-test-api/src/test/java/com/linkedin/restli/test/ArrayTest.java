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

package com.linkedin.restli.test;


import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneFacet;
import com.linkedin.restli.examples.greetings.client.ArrayTestBuilders;
import com.linkedin.restli.examples.greetings.client.ArrayTestFindByTestBuilder;
import org.testng.annotations.Test;

import java.util.Arrays;


/**
 * TODO: This test should be removed once the "items" field is fully deprecated
 *
 * @author Keren Jin
 */
public class ArrayTest
{
  @Test
  public void test()
  {
    final ArrayTestFindByTestBuilder builders = new ArrayTestBuilders().findByTest();
    builders.primitiveParam(Arrays.asList(1, 2, 3));
    builders.enumParam(Arrays.asList(Tone.FRIENDLY, Tone.SINCERE));
    builders.recordParam(Arrays.asList(new Message(), new Message()));
    builders.existingParam(Arrays.asList(new ToneFacet(), new ToneFacet()));
  }
}
