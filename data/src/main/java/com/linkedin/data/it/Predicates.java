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

package com.linkedin.data.it;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import java.util.Collection;


/**
 * Contains methods for creating {@link Predicate}'s.
 * <p>
 * Example usage:
 * <ul>
 * <li> {@code and(valueInstanceOf(Integer.class), nameEquals("i"))}
 * <li> {@code and(dataSchemaNameEquals("FooRecord"), hasChildWithNameChild("id", 52))}
 * <li> {@code or(nameEquals("foo"), nameEquals("bar"))}
 * </ul>
 */
public class Predicates
{
  public static Predicate valueInstanceOf(Class<?> clazz)
  {
    return new ValueInstanceOfPredicate(clazz);
  }

  public static Predicate nameEquals(Object name)
  {
    return new NameEqualsPredicate(name);
  }

  public static Predicate valueEquals(Object value)
  {
    return new ValueEqualsPredicate(value);
  }

  public static Predicate dataSchemaTypeEquals(DataSchema.Type type)
  {
    return new DataSchemaTypeEqualsPredicate(type);
  }

  public static Predicate dataSchemaNameEquals(String fullName)
  {
    return new DataSchemaNameEqualsPredicate(fullName);
  }

  public static Predicate hasChildWithNameValue(Object name, Object value)
  {
    return new HasChildWithNameValuePredicate(name, value);
  }

  public static Predicate pathMatchesPattern(Object... patterns)
  {
    return new PathMatchesPatternPredicate(patterns);
  }
  
  public static Predicate pathMatchesPathSpec(PathSpec pathSpec)
  {
    return new PathMatchesPatternPredicate(pathSpec);
  }

  public static Predicate and(Collection<? extends Predicate> predicates)
  {
    return new AndPredicate(predicates);
  }

  public static Predicate and(Predicate... predicates)
  {
    return new AndPredicate(predicates);
  }

  public static Predicate or(Collection<? extends Predicate> predicates)
  {
    return new OrPredicate(predicates);
  }

  public static Predicate or(Predicate... predicates)
  {
    return new OrPredicate(predicates);
  }

  public static Predicate parent(Predicate predicate)
  {
    return new ParentPredicate(predicate);
  }

  public static Predicate alwaysTrue()
  {
    return ALWAYS_TRUE;
  }

  public static Predicate alwaysFalse()
  {
    return ALWAYS_FALSE;
  }

  private static final Predicate ALWAYS_TRUE = new AlwaysTruePredicate();
  private static final Predicate ALWAYS_FALSE = new AlwaysFalsePredicate();
}
