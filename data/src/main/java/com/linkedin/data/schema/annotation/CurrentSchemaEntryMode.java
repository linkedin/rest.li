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
package com.linkedin.data.schema.annotation;

/**
 * During schema traversal, this enum tells how the current schema being visited is linked from its parentSchema as a child schema
 * Used by {@link DataSchemaRichContextTraverser} and {@link TraverserContext}
 */
enum CurrentSchemaEntryMode
{
  // child schema is for a record's field
  FIELD,
  // child schema is the key field of map
  MAP_KEY,
  // child schema is the value field of map
  MAP_VALUE,
  // child schema is the item of array
  ARRAY_VALUE,
  // child schema is a member of union
  UNION_MEMBER,
  // child schema is referred from a typeref schema
  TYPEREF_REF
}
