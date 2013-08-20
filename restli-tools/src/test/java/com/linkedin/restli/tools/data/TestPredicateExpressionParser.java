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

package com.linkedin.restli.tools.data;


import com.linkedin.data.it.AlwaysFalsePredicate;
import com.linkedin.data.it.AlwaysTruePredicate;
import com.linkedin.data.it.AndPredicate;
import com.linkedin.data.it.NotPredicate;
import com.linkedin.data.it.OrPredicate;
import com.linkedin.data.it.Predicate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.EmptyStackException;
import java.util.List;


/**
 * @author Keren Jin
 */
public class TestPredicateExpressionParser
{
  @Test
  public void testPredicate()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate");
    Assert.assertEquals(parsed.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNot()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!com.linkedin.data.it.AlwaysTruePredicate");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);
    Assert.assertEquals(((NotPredicate) parsed).getChildPredicate().getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testDoubleNot()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!!com.linkedin.data.it.AlwaysTruePredicate");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);

    final Predicate intermediate1 = ((NotPredicate) parsed).getChildPredicate();
    Assert.assertEquals(intermediate1.getClass(), NotPredicate.class);

    final Predicate intermediate2 = ((NotPredicate) intermediate1).getChildPredicate();
    Assert.assertEquals(intermediate2.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> children = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> children = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testAndAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> children = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(2).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testOrOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> children = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(2).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testAndOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AndPredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysFalsePredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) orChildren.get(0)).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testOrAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate & com.linkedin.data.it.AlwaysTruePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) orChildren.get(1)).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), AlwaysFalsePredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testAndOrAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate | com.linkedin.data.it.AlwaysFalsePredicate & com.linkedin.data.it.AlwaysTruePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AndPredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AndPredicate.class);

    final List<Predicate> andChildren0 = ((AndPredicate) orChildren.get(0)).getChildPredicates();
    Assert.assertEquals(andChildren0.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(andChildren0.get(1).getClass(), AlwaysFalsePredicate.class);

    final List<Predicate> andChildren1 = ((AndPredicate) orChildren.get(1)).getChildPredicates();
    Assert.assertEquals(andChildren1.get(0).getClass(), AlwaysFalsePredicate.class);
    Assert.assertEquals(andChildren1.get(1).getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testOrAndOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate & com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AndPredicate.class);
    Assert.assertEquals(orChildren.get(2).getClass(), AlwaysFalsePredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) orChildren.get(1)).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), AlwaysFalsePredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNotAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), NotPredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysFalsePredicate.class);

    final Predicate notChild = ((NotPredicate) andChildren.get(0)).getChildPredicate();
    Assert.assertEquals(notChild.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNotNotAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!!com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), NotPredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysFalsePredicate.class);

    final Predicate notChild1 = ((NotPredicate) andChildren.get(0)).getChildPredicate();
    Assert.assertEquals(notChild1.getClass(), NotPredicate.class);

    final Predicate notChild2 = ((NotPredicate) notChild1).getChildPredicate();
    Assert.assertEquals(notChild2.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testAndNot()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & !com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), NotPredicate.class);

    final Predicate notChild = ((NotPredicate) andChildren.get(1)).getChildPredicate();
    Assert.assertEquals(notChild.getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testNotNotOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!!com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), NotPredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysFalsePredicate.class);

    final Predicate notChild1 = ((NotPredicate) orChildren.get(0)).getChildPredicate();
    Assert.assertEquals(notChild1.getClass(), NotPredicate.class);

    final Predicate notChild2 = ((NotPredicate) notChild1).getChildPredicate();
    Assert.assertEquals(notChild2.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testOrNot()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate | !com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) parsed).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), NotPredicate.class);

    final Predicate notChild = ((NotPredicate) orChildren.get(1)).getChildPredicate();
    Assert.assertEquals(notChild.getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testParen()
  {
    final Predicate parsed = PredicateExpressionParser.parse("(com.linkedin.data.it.AlwaysTruePredicate)");
    Assert.assertEquals(parsed.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testDoubleParen()
  {
    final Predicate parsed = PredicateExpressionParser.parse("((com.linkedin.data.it.AlwaysTruePredicate))");
    Assert.assertEquals(parsed.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNotParen()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!(com.linkedin.data.it.AlwaysTruePredicate)");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);
    Assert.assertEquals(((NotPredicate) parsed).getChildPredicate().getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testDoubleNotParen()
  {
    final Predicate parsed = PredicateExpressionParser.parse("(!(!(com.linkedin.data.it.AlwaysTruePredicate)))");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);

    final Predicate intermediate1 = ((NotPredicate) parsed).getChildPredicate();
    Assert.assertEquals(intermediate1.getClass(), NotPredicate.class);

    final Predicate intermediate2 = ((NotPredicate) intermediate1).getChildPredicate();
    Assert.assertEquals(intermediate2.getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNotParenAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!(com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate)");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);

    final Predicate intermediate = ((NotPredicate) parsed).getChildPredicate();
    Assert.assertEquals(intermediate.getClass(), AndPredicate.class);

    final List<Predicate> children = ((AndPredicate) intermediate).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testNotParenOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!(com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate)");
    Assert.assertEquals(parsed.getClass(), NotPredicate.class);

    final Predicate intermediate = ((NotPredicate) parsed).getChildPredicate();
    Assert.assertEquals(intermediate.getClass(), OrPredicate.class);

    final List<Predicate> children = ((OrPredicate) intermediate).getChildPredicates();
    Assert.assertEquals(children.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(children.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testAndParenOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate & (com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate)");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) andChildren.get(1)).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testOrParenAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("(com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysTruePredicate) & com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), OrPredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), AlwaysFalsePredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) andChildren.get(0)).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysTruePredicate.class);
  }

  @Test
  public void testNotAndParenOr()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!com.linkedin.data.it.AlwaysTruePredicate & !(com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysFalsePredicate)");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), NotPredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), NotPredicate.class);

    final Predicate notChild1 = ((NotPredicate) andChildren.get(0)).getChildPredicate();
    Assert.assertEquals(notChild1.getClass(), AlwaysTruePredicate.class);

    final Predicate notChild2 = ((NotPredicate) andChildren.get(1)).getChildPredicate();
    Assert.assertEquals(notChild2.getClass(), OrPredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) notChild2).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysFalsePredicate.class);
  }

  @Test
  public void testNotOrParenAnd()
  {
    final Predicate parsed = PredicateExpressionParser.parse("!(com.linkedin.data.it.AlwaysTruePredicate | com.linkedin.data.it.AlwaysTruePredicate) & !com.linkedin.data.it.AlwaysFalsePredicate");
    Assert.assertEquals(parsed.getClass(), AndPredicate.class);

    final List<Predicate> andChildren = ((AndPredicate) parsed).getChildPredicates();
    Assert.assertEquals(andChildren.get(0).getClass(), NotPredicate.class);
    Assert.assertEquals(andChildren.get(1).getClass(), NotPredicate.class);

    final Predicate notChild1 = ((NotPredicate) andChildren.get(0)).getChildPredicate();
    Assert.assertEquals(notChild1.getClass(), OrPredicate.class);

    final Predicate notChild2 = ((NotPredicate) andChildren.get(1)).getChildPredicate();
    Assert.assertEquals(notChild2.getClass(), AlwaysFalsePredicate.class);

    final List<Predicate> orChildren = ((OrPredicate) notChild1).getChildPredicates();
    Assert.assertEquals(orChildren.get(0).getClass(), AlwaysTruePredicate.class);
    Assert.assertEquals(orChildren.get(1).getClass(), AlwaysTruePredicate.class);
  }

  /**
   * This is a ugly case that ! will apply even in suffix position. It is fixable in the parser but for simplicity we decide to not fix
   */
  @Test
  public void testOperandNotMissingOperandAnd()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate ! & com.linkedin.data.it.AlwaysFalsePredicate");
  }

  /**
   * This is a ugly case that ! will apply even in suffix position. It is fixable in the parser but for simplicity we decide to not fix
   */
  @Test
  public void testOperandNotMissingOperandOr()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate ! | com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testNotPredicate()
  {
    PredicateExpressionParser.parse("com.linkedin.restli.tools.data.PredicateExpressionParser");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testTooManyPredicates()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysTruePredicate com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testAndMissingOperand2()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysFalsePredicate & ");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testOrMissingOperand2()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysFalsePredicate | ");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testMissingOperand1And()
  {
    PredicateExpressionParser.parse("& com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testMissingOperand1Or()
  {
    PredicateExpressionParser.parse("| com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testNotMissingOperandAnd()
  {
    PredicateExpressionParser.parse("! & com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = EmptyStackException.class)
  public void testNotMissingOperandOr()
  {
    PredicateExpressionParser.parse("! | com.linkedin.data.it.AlwaysFalsePredicate)");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testMissingCloseParen()
  {
    PredicateExpressionParser.parse("(com.linkedin.data.it.AlwaysFalsePredicate");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testMissingOpenParen()
  {
    PredicateExpressionParser.parse("com.linkedin.data.it.AlwaysFalsePredicate)");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testTwoLeftInStack()
  {
    PredicateExpressionParser.parse("(com.linkedin.data.it.AlwaysTruePredicate & com.linkedin.data.it.AlwaysFalsePredicate)(com.linkedin.data.it.AlwaysFalsePredicate & com.linkedin.data.it.AlwaysTruePredicate)");
  }
}
