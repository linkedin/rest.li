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

package com.linkedin.restli.internal.server.model;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.AbstractArrayTemplate;
import com.linkedin.data.template.AbstractMapTemplate;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.ParSeqContext;
import com.linkedin.restli.server.resources.AssociationResource;
import com.linkedin.restli.server.resources.AssociationResourceAsync;
import com.linkedin.restli.server.resources.CollectionResource;
import com.linkedin.restli.server.resources.CollectionResourceAsync;
import com.linkedin.restli.server.resources.ComplexKeyResource;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsync;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author dellamag
 */
public interface RestModelConstants
{
  Class<?>[] VALID_QUERY_PARAMETER_TYPES = new Class<?>[] {
      String.class,
      boolean.class,
      Boolean.class,
      int.class,
      Integer.class,
      long.class,
      Long.class,
      float.class,
      Float.class,
      double.class,
      Double.class,
      Enum.class,
      ByteString.class,
      DataTemplate.class,

      String[].class,
      boolean[].class,
      Boolean[].class,
      int[].class,
      Integer[].class,
      long[].class,
      Long[].class,
      float[].class,
      Float[].class,
      double[].class,
      Double[].class,
      Enum[].class,
      DataTemplate[].class
  };

  /*
   * Unions are not allowed because returning union causes an "unnamed' union
   * to be emitted as the type of the argument. This will cause client code generation
   * to fail or alternatively generate a new union class for each occurrence of an union
   * as a param or return type.
   *
   * Generating another union class for each occurrence is not quite right either because
   * the union that was used in the server was declared as part of record or some other
   * type, and the generated union class would not be the same one as originally used
   * by the server.
   */

  Class<?>[] VALID_ACTION_PARAMETER_TYPES = new Class<?>[] {
      boolean.class,
      Boolean.class,
      int.class,
      Integer.class,
      long.class,
      Long.class,
      float.class,
      Float.class,
      double.class,
      Double.class,
      String.class,
      ByteString.class,
      Enum.class,
      RecordTemplate.class,
      FixedTemplate.class,
      AbstractArrayTemplate.class,
      AbstractMapTemplate.class
  };

  Class<?>[] VALID_ACTION_RETURN_TYPES = new Class<?>[] {
      Void.TYPE,
      boolean.class,
      Boolean.class,
      int.class,
      Integer.class,
      long.class,
      Long.class,
      float.class,
      Float.class,
      double.class,
      Double.class,
      String.class,
      ByteString.class,
      Enum.class,
      RecordTemplate.class,
      FixedTemplate.class,
      AbstractArrayTemplate.class,
      AbstractMapTemplate.class,
      ActionResult.class
  };

  @SuppressWarnings("serial")
  Map<DataSchema.Type, Class<?>[]> PRIMITIVE_DATA_SCHEMA_TYPE_ALLOWED_TYPES = new HashMap<DataSchema.Type, Class<?>[]>()
  {
    {
      put(DataSchema.Type.BOOLEAN, new Class[] { boolean.class, Boolean.class });
      put(DataSchema.Type.INT, new Class[] { int.class, Integer.class });
      put(DataSchema.Type.LONG, new Class[] { long.class, Long.class });
      put(DataSchema.Type.FLOAT, new Class[] { float.class, Float.class});
      put(DataSchema.Type.DOUBLE, new Class[] { double.class, Double.class});
      put(DataSchema.Type.STRING, new Class[] { String.class });
      put(DataSchema.Type.BYTES, new Class[] { ByteString.class });
    }
  };

  Class<?>[] FIXED_RESOURCE_CLASSES = {
      CollectionResource.class,
      CollectionResourceAsync.class,
      AssociationResource.class,
      AssociationResourceAsync.class,
      ComplexKeyResource.class,
      ComplexKeyResourceAsync.class
  };

  Set<Class<?>> CLASSES_WITHOUT_SCHEMAS = new HashSet<Class<?>>(
          Arrays.asList(
                  new Class<?>[]{ComplexResourceKey.class,
                          CompoundKey.class,
                          Context.class,
                          Callback.class,
                          PagingContext.class,
                          ParSeqContext.class}));
}
