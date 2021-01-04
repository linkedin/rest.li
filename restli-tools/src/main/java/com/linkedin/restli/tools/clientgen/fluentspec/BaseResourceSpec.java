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
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import java.io.File;


public class BaseResourceSpec
{
  final ResourceSchema _resource;
  final TemplateSpecGenerator _templateSpecGenerator;
  final String _sourceIdlName;
  final DataSchemaLocation _currentSchemaLocation;
  final DataSchemaResolver _schemaResolver;

  public BaseResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    _resource = resourceSchema;
    _templateSpecGenerator = templateSpecGenerator;
    _sourceIdlName = sourceIdlName;
    _schemaResolver = schemaResolver;
    _currentSchemaLocation = new FileDataSchemaLocation(new File(_sourceIdlName));
  }

  public ResourceSchema getResource()
  {
    return _resource;
  }

  public TemplateSpecGenerator getTemplateSpecGenerator()
  {
    return _templateSpecGenerator;
  }

  public String getSourceIdlName()
  {
    return _sourceIdlName;
  }

  public String getClassName()
  {
    return RestLiToolsUtils.nameCapsCase(_resource.getName());
  }

  public String getNamespace()
  {
    return _resource.hasNamespace() ? _resource.getNamespace() : "";
  }

  protected ClassTemplateSpec classToTemplateSpec(String classname)
  {
    if (classname == null || "Void".equals(classname))
    {
      return null;
    }
    else
    {
      final DataSchema typeSchema = RestSpecCodec.textToSchema(classname, _schemaResolver);
      return schemaToTemplateSpec(typeSchema);
    }
  }

  protected ClassTemplateSpec schemaToTemplateSpec(DataSchema dataSchema)
  {
    // convert from DataSchema to ClassTemplateSpec
    return _templateSpecGenerator.generate(dataSchema, _currentSchemaLocation);
  }
}
