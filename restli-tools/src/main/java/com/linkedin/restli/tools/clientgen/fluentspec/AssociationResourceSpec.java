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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ClassUtils.getShortClassName;


public class AssociationResourceSpec extends BaseResourceSpec
{
  private final CompoundKeySpec _compoundKeySpec;
  private Set<String> assockeyTypeImports = new LinkedHashSet<>(4); // import assocKeyTYpe if not primitive

  public AssociationResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);

    _compoundKeySpec = new CompoundKeySpec();

    for (AssocKeySchema assocKey: getResource().getAssociation().getAssocKeys())
    {
      String assocKeyType = assocKey.getType();
      DataSchema typeSchema = RestSpecCodec.textToSchema(assocKeyType, _schemaResolver);
      String javaBindTypeFull = getJavaBindTypeName(typeSchema);
      String declaredTypeFull = getClassRefNameForSchema(typeSchema);
      _compoundKeySpec.addAssocKeySpec(
          assocKey.getName(),
          assocKeyType,
          addToImportsAndShorten(javaBindTypeFull, assockeyTypeImports),
          addToImportsAndShorten(declaredTypeFull, assockeyTypeImports));
    }
  }

  @Override
  public List<RestMethodSpec> getRestMethods()
  {
    List<RestMethodSpec> methods = new ArrayList<>(getResource().getAssociation().getMethods().size());
    for (RestMethodSchema methodSchema : getResource().getAssociation().getMethods())
    {
      String methodType = methodSchema.getMethod().toUpperCase();
      if (methodType.equals(ResourceMethod.CREATE.name()) || methodType.equals(ResourceMethod.BATCH_CREATE.name()))
      {
        // Association resource never supports create and batch_create
        // create and batch_create in association resource will be skipped for now
        continue;
      }
      methods.add(new RestMethodSpec(methodSchema, this));
    }
    return methods;
  }

  @Override
  public List<ActionMethodSpec> getActions()
  {
    if (getResource().getAssociation().getActions() == null)
    {
      return Collections.emptyList();
    }
    List<ActionMethodSpec> actions = new ArrayList<>(getResource().getAssociation().getActions().size());
    getResource().getAssociation().getActions().forEach(actionSchema -> actions.add(new ActionMethodSpec(actionSchema, this)));
    return actions;
  }

  public String getIdName()
  {
    return getResource().getAssociation().getIdentifier();
  }

  public String getKeyClassName()
  {
    return getShortClassName(CompoundKey.class.getName());
  }

  @Override
  public Set<String> getResourceSpecificImports(Set<String> imports)
  {
    imports.add(CompoundKey.class.getName());
    imports.addAll(assockeyTypeImports);
    return imports;
  }

  public CompoundKeySpec getCompoundKeySpec()
  {
    return _compoundKeySpec;
  }

  /**
   * To return the shortened type name; If the type is not primitive,
   * this method will also add that to the imports set.
   * @param fullType the full type name being checked
   * @param imports the imports set to add to
   * @return shorted type name
   */
  private String addToImportsAndShorten(String fullType, Set<String> imports)
  {
    if (!fullType.startsWith("java.lang"))
    {
      imports.add(fullType);
    }
    return getShortClassName(fullType);
  }

}
