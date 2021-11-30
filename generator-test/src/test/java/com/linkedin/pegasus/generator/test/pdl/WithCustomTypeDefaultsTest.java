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

import com.linkedin.pegasus.generator.test.idl.records.WithCustomTypeDefaults;
import org.testng.annotations.Test;


public class WithCustomTypeDefaultsTest
{
  @Test
  public void testInitializeWithCustomTypeDefaults()
  {
    // Regression test to ensure that a type with a custom type and coercer can
    // load properly. This was broken once because static statements in the code
    // that was generated were out of order.
    //
    // It's important for the function of this test that the custom type and its
    // coercer defined in the PDL are only used in this PDL to make sure that no
    // other code could accidentally initialize the coercer and trick this test
    // into thinking everything is fine.
    //
    new WithCustomTypeDefaults();
  }
}
