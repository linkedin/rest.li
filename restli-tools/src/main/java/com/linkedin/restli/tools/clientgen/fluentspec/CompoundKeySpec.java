/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.fluentspec;

import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import java.util.ArrayList;
import java.util.List;


/**
 * Spec for Compound Key, used by {@link AssociationResourceSpec}
 */
public class CompoundKeySpec
{
  private List<AssocKeySpec> assocKeySpecs = new ArrayList<>(4);

  public List<AssocKeySpec> getAssocKeySpecs()
  {
    return assocKeySpecs;
  }

  public void addAssocKeySpec(String name, String type, String bindingType, String declaredType)
  {
    assocKeySpecs.add(new AssocKeySpec(name,type,bindingType,declaredType));
  }

  /**
   * Spec for one association key in the Compound key
   */
  public static class AssocKeySpec
  {
    private final String name;
    private final String type;
    private final String bindingType;
    private final String declaredType;

    public String getName()
    {
      return name;
    }

    public String getType()
    {
      return type;
    }

    public String getBindingType()
    {
      return bindingType;
    }

    public String getDeclaredType()
    {
      return declaredType;
    }

    /**
     * @param name name of the association key, Also check AssociationKeySchema.pdl
     * @param type the schema type of this key as defined in the resource, e.g. "int", or the typeref type when typeRef
     *             is used. Also check AssociationKeySchema.pdl
     * @param bindingType the java bind type, for example, Integer.java will be used for the "int" used in the schema,
     *                    or it could be the custom type if the typeref to this custom type is also specified.
     * @param declaredType the type used when this key is declared, this can be different from bindingType, for example,
     *                     when typeref is used, declaredType will be that typeref; This could also be different from
     *                     schema type, for example when schema type is "int", declaredType would be "Integer"
     */
    public AssocKeySpec(String name, String type, String bindingType, String declaredType)
    {
      this.name = name;
      this.type = type;
      this.bindingType = bindingType;
      this.declaredType = declaredType;
    }
  }
}
