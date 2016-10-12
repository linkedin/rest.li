/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.spec;


import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.BindingInfo;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.UnionDataSchema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Keren Jin
 */
public class ClassTemplateSpec implements BindingInfo
{
  private DataSchema _schema;
  private TyperefDataSchema _originalTyperefSchema;
  private ClassTemplateSpec _enclosingClass;
  private String _namespace;
  private String _className;
  private String _package;
  private Set<ModifierSpec> _modifiers;
  private String _location;

  public static ClassTemplateSpec createFromDataSchema(DataSchema schema)
  {
    if (schema instanceof ArrayDataSchema)
    {
      return new ArrayTemplateSpec((ArrayDataSchema) schema);
    }
    else if (schema instanceof EnumDataSchema)
    {
      return new EnumTemplateSpec((EnumDataSchema) schema);
    }
    else if (schema instanceof FixedDataSchema)
    {
      return new FixedTemplateSpec((FixedDataSchema) schema);
    }
    else if (schema instanceof MapDataSchema)
    {
      return new MapTemplateSpec((MapDataSchema) schema);
    }
    else if (schema instanceof PrimitiveDataSchema)
    {
      return PrimitiveTemplateSpec.getInstance(schema.getType());
    }
    else if (schema instanceof RecordDataSchema)
    {
      return new RecordTemplateSpec((RecordDataSchema) schema);
    }
    else if (schema instanceof TyperefDataSchema)
    {
      return new TyperefTemplateSpec((TyperefDataSchema) schema);
    }
    else if (schema instanceof UnionDataSchema)
    {
      return new UnionTemplateSpec((UnionDataSchema) schema);
    }
    else
    {
      throw new RuntimeException();
    }
  }

  public DataSchema getSchema()
  {
    return _schema;
  }

  public void setSchema(DataSchema schema)
  {
    _schema = schema;
  }

  public TyperefDataSchema getOriginalTyperefSchema()
  {
    return _originalTyperefSchema;
  }

  public void setOriginalTyperefSchema(TyperefDataSchema originalTyperefSchema)
  {
    _originalTyperefSchema = originalTyperefSchema;
  }

  public ClassTemplateSpec getEnclosingClass()
  {
    return _enclosingClass;
  }

  public void setEnclosingClass(ClassTemplateSpec enclosingClass)
  {
    _enclosingClass = enclosingClass;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setNamespace(String namespace)
  {
    this._namespace = namespace;
  }

  @Override
  public String getPackage()
  {
    return (_package == null || _package.isEmpty()) ? _namespace : _package;
  }

  public void setPackage(String packageName)
  {
    _package = packageName;
  }

  public String getClassName()
  {
    return _className;
  }

  public void setClassName(String className)
  {
    this._className = className;
  }

  public Set<ModifierSpec> getModifiers()
  {
    return _modifiers;
  }

  public void setModifiers(ModifierSpec... modifiers)
  {
    _modifiers = new HashSet<ModifierSpec>(Arrays.asList(modifiers));
  }

  public String getLocation()
  {
    return _location;
  }

  public void setLocation(String location)
  {
    _location = location;
  }

  public String getFullName()
  {
    return (_namespace == null || _namespace.isEmpty()) ? _className : _namespace + "." + _className;
  }

  @Override
  public String getBindingName() {
    return (_package == null || _package.isEmpty()) ? getFullName() : _package + "." + _className;
  }

  public void setFullName(String fullName)
  {
    final int dotIndex = fullName.lastIndexOf('.');
    _namespace = dotIndex == -1 ? null : fullName.substring(0, dotIndex);
    _className = fullName.substring(dotIndex + 1);
  }
}
