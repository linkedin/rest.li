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

package com.linkedin.data.avro;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;


/**
 * Compare 2 Avro schemas for equality.
 *
 * NOTE: This is temporary until avro-util supports an equality check between 2 avro schemas
 */
public class AvroSchemaEquals
{
  private AvroSchemaEquals()
  {
    //utility class
  }

  public static boolean equals(Schema a, Schema b, boolean considerStringJsonProps, boolean considerNonStringJsonProps,
      boolean considerAliases)
  {
    return equals(a, b, considerStringJsonProps, considerNonStringJsonProps, considerAliases, new HashSet<>(3));
  }

  private static boolean equals(Schema a, Schema b, boolean considerStringJsonProps, boolean considerNonStringJsonProps,
      boolean considerAliases, Set<SeenPair> seen)
  {
    if (a == null && b == null)
    {
      return true;
    }
    if (a == null || b == null)
    {
      return false;
    }
    Schema.Type type = a.getType();
    if (!Objects.equals(type, b.getType()))
    {
      return false;
    }
    switch (type)
    {
      //all of these have nothing more to compare by beyond their type (and we ignore props)
      case NULL:
        return true;
      case BOOLEAN:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case STRING:
      case BYTES:
        return true;

      //named types

      case ENUM:
        return a.getFullName().equals(b.getFullName()) && (!considerAliases || hasSameAliases(a, b))
            && a.getEnumSymbols().equals(b.getEnumSymbols());
      case FIXED:
        return a.getFullName().equals(b.getFullName()) && (!considerAliases || hasSameAliases(a, b))
            && a.getFixedSize() == b.getFixedSize();
      case RECORD:
        return recordSchemaEquals(a, b, considerStringJsonProps, considerNonStringJsonProps, considerAliases, seen);

      //collections and union

      case ARRAY:
        return equals(a.getElementType(), b.getElementType(), considerStringJsonProps, considerNonStringJsonProps,
            considerAliases, seen);
      case MAP:
        return equals(a.getValueType(), b.getValueType(), considerStringJsonProps, considerNonStringJsonProps,
            considerAliases, seen);
      case UNION:
        List<Schema> aBranches = a.getTypes();
        List<Schema> bBranches = b.getTypes();
        if (aBranches.size() != bBranches.size())
        {
          return false;
        }
        for (int i = 0; i < aBranches.size(); i++)
        {
          Schema aBranch = aBranches.get(i);
          Schema bBranch = bBranches.get(i);
          if (!equals(aBranch, bBranch, considerStringJsonProps, considerNonStringJsonProps, considerAliases, seen))
          {
            return false;
          }
        }
        return true;
      default:
        throw new IllegalStateException("unhandled: " + type);
    }
  }

  private static boolean recordSchemaEquals(Schema a, Schema b, boolean considerStringJsonProps,
      boolean considerNonStringJsonProps, boolean considerAliases, Set<SeenPair> seen)
  {
    if (!a.getFullName().equals(b.getFullName()))
    {
      return false;
    }
    //loop protection for self-referencing schemas
    SeenPair pair = new SeenPair(a, b);
    if (seen.contains(pair))
    {
      return true;
    }
    seen.add(pair);
    try
    {
      if (considerAliases && !hasSameAliases(a, b))
      {
        return false;
      }

      if (!hasSameObjectProps(a, b, considerStringJsonProps, considerNonStringJsonProps))
      {
        return false;
      }

      List<Schema.Field> aFields = a.getFields();
      List<Schema.Field> bFields = b.getFields();
      if (aFields.size() != bFields.size())
      {
        return false;
      }
      for (int i = 0; i < aFields.size(); i++)
      {
        Schema.Field aField = aFields.get(i);
        Schema.Field bField = bFields.get(i);

        if (!aField.name().equals(bField.name()))
        {
          return false;
        }
        if (!equals(aField.schema(), bField.schema(), considerStringJsonProps, considerNonStringJsonProps,
            considerAliases, seen))
        {
          return false;
        }
        if (AvroCompatibilityHelper.fieldHasDefault(aField) && AvroCompatibilityHelper.fieldHasDefault(bField))
        {
          //TODO - this is potentially an issue since it would call vanilla equals() between the schemas of the default values
          Object aDefaultValue = AvroCompatibilityHelper.getGenericDefaultValue(aField);
          Object bDefaultValue = AvroCompatibilityHelper.getGenericDefaultValue(bField);
          if (!Objects.equals(aDefaultValue, bDefaultValue))
          {
            return false;
          }
        } else if (AvroCompatibilityHelper.fieldHasDefault(aField) || AvroCompatibilityHelper.fieldHasDefault(bField))
        {
          //means one field has a default value and the other does not
          return false;
        }

        if (!Objects.equals(aField.order(), bField.order()))
        {
          return false;
        }

        if (!hasSameObjectProps(aField, bField, considerStringJsonProps, considerNonStringJsonProps))
        {
          return false;
        }
      }
      return true;
    } finally
    {
      seen.remove(pair);
    }
  }

  private static boolean hasSameAliases(Schema a, Schema b)
  {
    return a.getAliases().equals(b.getAliases());
  }

  private static boolean hasSameObjectProps(JsonProperties a, JsonProperties b, boolean compareStringProps,
      boolean compareNonStringProps)
  {
    if (!compareStringProps && !compareNonStringProps)
    {
      return true;  // They do have the same props if you ignore everything
    }

    //TODO - getObjectProps() is expensive. find cheaper way?
    Map<String, Object> aProps = a.getObjectProps();
    Map<String, Object> bProps = b.getObjectProps();

    if (compareStringProps && compareNonStringProps)
    {
      return aProps.equals(bProps);
    }

    if (compareStringProps)
    {
      Map<String, CharSequence> aStringProps = new HashMap<>(aProps.size());
      aProps.forEach((k, v) ->
      {
        if (v instanceof CharSequence)
        {
          aStringProps.put(k, (CharSequence) v);
        }
      });
      Map<String, CharSequence> bStringProps = new HashMap<>(bProps.size());
      bProps.forEach((k, v) ->
      {
        if (v instanceof CharSequence)
        {
          bStringProps.put(k, (CharSequence) v);
        }
      });

      if (!aStringProps.equals(bStringProps))
      {
        return false;
      }
    }

    if (compareNonStringProps)
    {
      Map<String, Object> aNonStringProps = new HashMap<>(aProps.size());
      aProps.forEach((k, v) ->
      {
        if (!(v instanceof CharSequence))
        {
          aNonStringProps.put(k, v);
        }
      });
      Map<String, Object> bNonStringProps = new HashMap<>(bProps.size());
      bProps.forEach((k, v) ->
      {
        if (!(v instanceof CharSequence))
        {
          bNonStringProps.put(k, v);
        }
      });

      return aNonStringProps.equals(bNonStringProps);
    }

    return true;
  }

  private static class SeenPair
  {
    private final Schema s1;
    private final Schema s2;

    public SeenPair(Schema s1, Schema s2)
    {
      this.s1 = s1;
      this.s2 = s2;
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof SeenPair))
      {
        return false;
      }
      return this.s1 == ((SeenPair) o).s1 && this.s2 == ((SeenPair) o).s2;
    }

    @Override
    public int hashCode()
    {
      return System.identityHashCode(s1) + System.identityHashCode(s2);
    }
  }
}
