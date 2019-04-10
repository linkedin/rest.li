/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.sample;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.tools.sample.CustomKeyAssociationRequestBuilders;
import com.linkedin.restli.tools.sample.CustomLong;
import com.linkedin.restli.tools.sample.CustomLongRef;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


/**
 * Test generated compound key builder with custom key parts
 *
 * @author Karthik B
 */
public class TestCustomTypeCompoundKeyBuilder
{
  @Test
  public void testKeyBuilder()
  {
    CustomKeyAssociationRequestBuilders.Key key = Mockito.spy(new CustomKeyAssociationRequestBuilders.Key());

    CustomLong customLong = new CustomLong(1234L);
    key.setDateId("01/01/2019");
    key.setLongId(customLong);

    ArgumentCaptor<CompoundKey.TypeInfo> typeInfoArgumentCaptor = ArgumentCaptor.forClass(CompoundKey.TypeInfo.class);
    verify(key).append(eq("longId"), same(customLong), typeInfoArgumentCaptor.capture());

    Assert.assertEquals(CustomLong.class, typeInfoArgumentCaptor.getValue().getBindingType());
    Assert.assertEquals(CustomLongRef.class, typeInfoArgumentCaptor.getValue().getDeclaredType());
  }

}
