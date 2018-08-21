/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.tools.returnentity;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;


/**
 * Dummy RecordTemplates for "return entity" resources.
 *
 * @author Evan Williams
 */
public class ReturnEntityTestDataModels
{
  public static class ReturnMe extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("com.linkedin.restli.tools.returnentity.ReturnMe", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public ReturnMe()
    {
      super(new DataMap(), SCHEMA);
    }

    public ReturnMe(DataMap map)
    {
      super(map, SCHEMA);
    }
  }
}
