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


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.UnionDataSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Keren Jin
 */
public class UnionTemplateSpec extends ClassTemplateSpec
{
  private List<Member> _members;
  private TyperefTemplateSpec _typerefClass;

  public UnionTemplateSpec(UnionDataSchema schema)
  {
    setSchema(schema);
    _members = new ArrayList<Member>();
  }

  @Override
  public UnionDataSchema getSchema()
  {
    return (UnionDataSchema) super.getSchema();
  }

  public List<Member> getMembers()
  {
    return _members;
  }

  public TyperefTemplateSpec getTyperefClass()
  {
    return _typerefClass;
  }

  public void setTyperefClass(TyperefTemplateSpec typerefClass)
  {
    _typerefClass = typerefClass;
  }

  public static class Member
  {
    private DataSchema _schema;
    private ClassTemplateSpec _classTemplateSpec;
    private ClassTemplateSpec _dataClass;

    public DataSchema getSchema()
    {
      return _schema;
    }

    public void setSchema(DataSchema schema)
    {
      _schema = schema;
    }

    public ClassTemplateSpec getClassTemplateSpec()
    {
      return _classTemplateSpec;
    }

    public void setClassTemplateSpec(ClassTemplateSpec classTemplateSpec)
    {
      _classTemplateSpec = classTemplateSpec;
    }

    public ClassTemplateSpec getDataClass()
    {
      return _dataClass;
    }

    public void setDataClass(ClassTemplateSpec dataClass)
    {
      _dataClass = dataClass;
    }
  }
}