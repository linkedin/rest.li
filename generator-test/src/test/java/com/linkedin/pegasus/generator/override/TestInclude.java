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

package com.linkedin.pegasus.generator.override;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;


/**
 * @author Min Chen
 */
public class TestInclude
{
  private static final String _includeABindName = IncludeA.class.getCanonicalName();
  private static final String _includeBBindName = IncludeB.class.getCanonicalName();
  private static final String _includeCBindName = IncludeC.class.getCanonicalName();
  private static final String _includeDBindName = IncludeD.class.getCanonicalName();

  private static final String _includeCRefBindName = IncludeCRef.class.getCanonicalName();
  private static final String _includeTypeRefBindName = IncludeTypeRef.class.getCanonicalName();
  private static final String _includeMultipleBindName = IncludeMultiple.class.getCanonicalName();

  @Test
  public void testIncludeB()
  {
    IncludeB b = new IncludeB();

    // fields defined in IncludeA are present in IncludeB
    b.setA1(1);
    b.setA2("a2");
    assertEquals(b.getA1(), new Integer(1));
    assertEquals(b.getA2(), "a2");

    // fields defined in IncludeB.
    b.setB1(2);
    b.setB2("b2");
    assertEquals(b.getB1(), new Integer(2));
    assertEquals(b.getB2(), "b2");

    // include has IncludeA.
    assertNotEquals(b.schema().getInclude().get(0).getFullName(), _includeABindName);
    assertEquals(b.schema().getInclude().get(0).getBindingName(), _includeABindName);

    // fields know where they are defined in
    assertNotEquals(b.schema().getField("a1").getRecord().getFullName(), _includeABindName);
    assertEquals(b.schema().getField("a1").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(b.schema().getField("a2").getRecord().getFullName(), _includeABindName);
    assertEquals(b.schema().getField("a2").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(b.schema().getField("b1").getRecord().getFullName(), _includeBBindName);
    assertEquals(b.schema().getField("b1").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(b.schema().getField("b2").getRecord().getFullName(), _includeBBindName);
    assertEquals(b.schema().getField("b2").getRecord().getBindingName(), _includeBBindName);

    // generated classes are not subclasses of each other.
    assertFalse(IncludeA.class.isAssignableFrom(IncludeB.class));
    assertFalse(IncludeB.class.isAssignableFrom(IncludeA.class));
  }

  @Test
  public void testIncludeC()
  {
    IncludeC c = new IncludeC();

    // fields defined in IncludeA are present in IncludeB
    c.setA1(1);
    c.setA2("a2");
    assertEquals(c.getA1(), new Integer(1));
    assertEquals(c.getA2(), "a2");

    // fields defined in IncludeB.
    c.setB1(2);
    c.setB2("b2");
    assertEquals(c.getB1(), new Integer(2));
    assertEquals(c.getB2(), "b2");

    // fields defined in IncludeC.
    c.setC1(3);
    c.setC2("c2");
    assertEquals(c.getC1(), new Integer(3));
    assertEquals(c.getC2(), "c2");

    // include contains IncludeB
    assertNotEquals(c.schema().getInclude().get(0).getFullName(), _includeBBindName);
    assertEquals(c.schema().getInclude().get(0).getBindingName(), _includeBBindName);

    // fields know where they are defined in
    assertNotEquals(c.schema().getField("a1").getRecord().getFullName(), _includeABindName);
    assertEquals(c.schema().getField("a1").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(c.schema().getField("a2").getRecord().getFullName(), _includeABindName);
    assertEquals(c.schema().getField("a2").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(c.schema().getField("b1").getRecord().getFullName(), _includeBBindName);
    assertEquals(c.schema().getField("b1").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(c.schema().getField("b2").getRecord().getFullName(), _includeBBindName);
    assertEquals(c.schema().getField("b2").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(c.schema().getField("c1").getRecord().getFullName(), _includeCBindName);
    assertEquals(c.schema().getField("c2").getRecord().getBindingName(), _includeCBindName);

    // generated classes are not subclasses of each other.
    assertFalse(IncludeA.class.isAssignableFrom(IncludeB.class));
    assertFalse(IncludeA.class.isAssignableFrom(IncludeC.class));

    assertFalse(IncludeB.class.isAssignableFrom(IncludeA.class));
    assertFalse(IncludeB.class.isAssignableFrom(IncludeC.class));

    assertFalse(IncludeC.class.isAssignableFrom(IncludeA.class));
    assertFalse(IncludeC.class.isAssignableFrom(IncludeB.class));
  }

  @Test
  public void testIncludeMultiple()
  {
    IncludeMultiple m = new IncludeMultiple();

    // fields defined in IncludeA are present in IncludeB
    m.setA1(1);
    m.setA2("a2");
    assertEquals(m.getA1(), new Integer(1));
    assertEquals(m.getA2(), "a2");

    // fields defined in IncludeB.
    m.setB1(2);
    m.setB2("b2");
    assertEquals(m.getB1(), new Integer(2));
    assertEquals(m.getB2(), "b2");

    // fields defined in IncludeC.
    m.setC1(3);
    m.setC2("c2");
    assertEquals(m.getC1(), new Integer(3));
    assertEquals(m.getC2(), "c2");

    // fields defined in IncludeD.
    m.setD1(4);
    m.setD2("d2");
    assertEquals(m.getD1(), new Integer(4));
    assertEquals(m.getD2(), "d2");

    // fields defined in IncludeMultiple.
    m.setM1(5);
    m.setM2("m2");
    assertEquals(m.getM1(), new Integer(5));
    assertEquals(m.getM2(), "m2");

    // include contains IncludeC and IncludeD
    assertNotEquals(m.schema().getInclude().get(0).getFullName(), _includeCBindName);
    assertEquals(m.schema().getInclude().get(0).getBindingName(), _includeCBindName);
    assertNotEquals(m.schema().getInclude().get(1).getFullName(), _includeDBindName);
    assertEquals(m.schema().getInclude().get(1).getBindingName(), _includeDBindName);

    // fields know where they are defined in
    assertNotEquals(m.schema().getField("a1").getRecord().getFullName(), _includeABindName);
    assertEquals(m.schema().getField("a1").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(m.schema().getField("a2").getRecord().getFullName(), _includeABindName);
    assertEquals(m.schema().getField("a2").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(m.schema().getField("b1").getRecord().getFullName(), _includeBBindName);
    assertEquals(m.schema().getField("b1").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(m.schema().getField("b2").getRecord().getFullName(), _includeBBindName);
    assertEquals(m.schema().getField("b2").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(m.schema().getField("c1").getRecord().getFullName(), _includeCBindName);
    assertEquals(m.schema().getField("c1").getRecord().getBindingName(), _includeCBindName);
    assertNotEquals(m.schema().getField("c2").getRecord().getFullName(), _includeCBindName);
    assertEquals(m.schema().getField("c2").getRecord().getBindingName(), _includeCBindName);
    assertNotEquals(m.schema().getField("d1").getRecord().getFullName(), _includeDBindName);
    assertEquals(m.schema().getField("d1").getRecord().getBindingName(), _includeDBindName);
    assertNotEquals(m.schema().getField("d2").getRecord().getFullName(), _includeDBindName);
    assertEquals(m.schema().getField("d2").getRecord().getBindingName(), _includeDBindName);
    assertNotEquals(m.schema().getField("m1").getRecord().getFullName(), _includeMultipleBindName);
    assertEquals(m.schema().getField("m1").getRecord().getBindingName(), _includeMultipleBindName);
    assertNotEquals(m.schema().getField("m2").getRecord().getFullName(), _includeMultipleBindName);
    assertEquals(m.schema().getField("m2").getRecord().getBindingName(), _includeMultipleBindName);

    // generated classes are not subclasses of each other.
    assertFalse(IncludeA.class.isAssignableFrom(IncludeMultiple.class));
    assertFalse(IncludeB.class.isAssignableFrom(IncludeMultiple.class));
    assertFalse(IncludeC.class.isAssignableFrom(IncludeMultiple.class));
    assertFalse(IncludeD.class.isAssignableFrom(IncludeMultiple.class));

    assertFalse(IncludeMultiple.class.isAssignableFrom(IncludeA.class));
    assertFalse(IncludeMultiple.class.isAssignableFrom(IncludeB.class));
    assertFalse(IncludeMultiple.class.isAssignableFrom(IncludeC.class));
    assertFalse(IncludeMultiple.class.isAssignableFrom(IncludeD.class));
  }

  @Test
  public void testIncludeTyperef()
  {
    IncludeTypeRef t = new IncludeTypeRef();

    // fields defined in IncludeA are present in IncludeB
    t.setA1(1);
    t.setA2("a2");
    assertEquals(t.getA1(), new Integer(1));
    assertEquals(t.getA2(), "a2");

    // fields defined in IncludeB.
    t.setB1(2);
    t.setB2("b2");
    assertEquals(t.getB1(), new Integer(2));
    assertEquals(t.getB2(), "b2");

    // fields defined in IncludeC.
    t.setC1(3);
    t.setC2("c2");
    assertEquals(t.getC1(), new Integer(3));
    assertEquals(t.getC2(), "c2");

    // fields defined in IncludeTypeRef.
    t.setT1(4);
    t.setT2("t2");
    assertEquals(t.getT1(), new Integer(4));
    assertEquals(t.getT2(), "t2");

    // include contains IncludeRef
    assertNotEquals(t.schema().getInclude().get(0).getFullName(), _includeCRefBindName);
    assertEquals(t.schema().getInclude().get(0).getBindingName(), _includeCRefBindName);

    // fields know where they are defined in
    assertNotEquals(t.schema().getField("a1").getRecord().getFullName(), _includeABindName);
    assertEquals(t.schema().getField("a1").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(t.schema().getField("a2").getRecord().getFullName(), _includeABindName);
    assertEquals(t.schema().getField("a2").getRecord().getBindingName(), _includeABindName);
    assertNotEquals(t.schema().getField("b1").getRecord().getFullName(), _includeBBindName);
    assertEquals(t.schema().getField("b1").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(t.schema().getField("b2").getRecord().getFullName(), _includeBBindName);
    assertEquals(t.schema().getField("b2").getRecord().getBindingName(), _includeBBindName);
    assertNotEquals(t.schema().getField("c1").getRecord().getFullName(), _includeCBindName);
    assertEquals(t.schema().getField("c1").getRecord().getBindingName(), _includeCBindName);
    assertNotEquals(t.schema().getField("c2").getRecord().getFullName(), _includeCBindName);
    assertEquals(t.schema().getField("c2").getRecord().getBindingName(), _includeCBindName);
    assertNotEquals(t.schema().getField("t1").getRecord().getFullName(), _includeTypeRefBindName);
    assertEquals(t.schema().getField("t1").getRecord().getBindingName(), _includeTypeRefBindName);
    assertNotEquals(t.schema().getField("t2").getRecord().getFullName(), _includeTypeRefBindName);
    assertEquals(t.schema().getField("t2").getRecord().getBindingName(), _includeTypeRefBindName);

    // generated classes are not subclasses of each other.
    assertFalse(IncludeA.class.isAssignableFrom(IncludeB.class));
    assertFalse(IncludeA.class.isAssignableFrom(IncludeC.class));
    assertFalse(IncludeA.class.isAssignableFrom(IncludeTypeRef.class));

    assertFalse(IncludeB.class.isAssignableFrom(IncludeA.class));
    assertFalse(IncludeB.class.isAssignableFrom(IncludeC.class));
    assertFalse(IncludeB.class.isAssignableFrom(IncludeTypeRef.class));

    assertFalse(IncludeC.class.isAssignableFrom(IncludeA.class));
    assertFalse(IncludeC.class.isAssignableFrom(IncludeB.class));
    assertFalse(IncludeC.class.isAssignableFrom(IncludeTypeRef.class));
  }
}
