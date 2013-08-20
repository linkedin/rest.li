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


import com.linkedin.data.it.Predicate;
import com.linkedin.data.it.Predicates;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * <p>Parse boolean expression of {@link Predicate} names to a {@link Predicate}.</p>
 *
 * <p>Three boolean operators are supported: & (AND), | (OR) and ! (NOT). They are mapped to {@link com.linkedin.data.it.AndPredicate}, {@link com.linkedin.data.it.OrPredicate} and {@link com.linkedin.data.it.NotPredicate}, respectively. The precedence from high to low is ! > & > |.</p>
 *
 * <p>The expression is in infix style, therefore operands are specified around operators. For example, A & !B is parsed as AndPredicate(A, NotPredicate(B)).</p>
 *
 * <p>The expression is parsed from left to right. Parenthesis is allowed to change combination order.  For example, A | B & C is parsed as OrPredicate(A, AndPredicate(B, C)), while (A | B) & C is AndPredicate(OrPredicate(A, B), C)).</p>
 *
 * <p>Whitespace characters are removed before parsing, therefore "A & B" is equivalent to "A&B".</p>
 *
 * <p>All class name must implement {@link Predicate} and takes no parameter in constructor. Otherwise exception is thrown.</p>
 *
 * @author Keren Jin
 */
public class PredicateExpressionParser
{
  public static Predicate parse(String expression)
  {
    final Stack<Predicate> predicateStack = new Stack<Predicate>();
    final Stack<Character> operatorStack = new Stack<Character>();

    final String trimmedExpression = expression.replaceAll("\\s", "");
    final StringTokenizer tokenizer = new StringTokenizer(trimmedExpression, OPERATORS, true);
    boolean isTokenMode = true;

    while (true)
    {
      final Character operator;
      final String token;

      if (isTokenMode)
      {
        if (tokenizer.hasMoreTokens())
        {
          token = tokenizer.nextToken();
        }
        else
        {
          break;
        }

        if (OPERATORS.contains(token))
        {
          operator = token.charAt(0);
        }
        else
        {
          operator = null;
        }
      }
      else
      {
        operator = operatorStack.pop();
        token = null;
      }
      isTokenMode = true;

      if (operator == null)
      {
        try
        {
          predicateStack.push(Class.forName(token).asSubclass(Predicate.class).newInstance());
        }
        catch (ClassCastException e)
        {
          throw new RuntimeException(token + " must implement " + Predicate.class.getName(), e);
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
      else
      {
        if (operatorStack.empty() || operator == '(')
        {
          operatorStack.push(operator);
        }
        else if (operator == ')')
        {
          while (operatorStack.peek() != '(')
          {
            evaluate(predicateStack, operatorStack);
          }

          operatorStack.pop();
        }
        else
        {
          if (OPERATOR_PRECEDENCE.get(operator) < OPERATOR_PRECEDENCE.get(operatorStack.peek()))
          {
            evaluate(predicateStack, operatorStack);
            isTokenMode = false;
          }

          operatorStack.push(operator);
        }
      }
    }

    while (!operatorStack.empty())
    {
      evaluate(predicateStack, operatorStack);
    }

    if (predicateStack.size() > 1)
    {
      throw new RuntimeException("Invalid logical expression");
    }

    return predicateStack.pop();
  }

  private static void evaluate(Stack<Predicate> predicateStack, Stack<Character> operatorStack)
  {
    final char operator = operatorStack.pop();
    final Predicate evaluatedPredicate;

    switch (operator)
    {
      case '&':
      case '|':
        evaluatedPredicate = evaluateMultiaryOperator(predicateStack, operatorStack, operator);
        break;
      case '!':
        evaluatedPredicate = Predicates.not(predicateStack.pop());
        break;
      default:
        throw new RuntimeException("Unknown operator: " + operator);
    }

    predicateStack.push(evaluatedPredicate);
  }

  private static Predicate evaluateMultiaryOperator(Stack<Predicate> predicateStack, Stack<Character> operatorStack, char operator)
  {
    final Deque<Predicate> predicateOperands = new ArrayDeque<Predicate>();
    predicateOperands.addFirst(predicateStack.pop());
    predicateOperands.addFirst(predicateStack.pop());

    while (!operatorStack.empty() && operator == operatorStack.peek())
    {
      predicateOperands.addFirst(predicateStack.pop());
      operatorStack.pop();
    }

    switch (operator)
    {
      case '&':
        return Predicates.and(predicateOperands);
      case '|':
        return Predicates.or(predicateOperands);
      default:
        throw new RuntimeException("Logic error");
    }
  }

  private static final String OPERATORS = "()!&|";
  private static final Map<Character, Integer> OPERATOR_PRECEDENCE = new HashMap<Character, Integer>();
  static
  {
    OPERATOR_PRECEDENCE.put('(', 0);
    OPERATOR_PRECEDENCE.put('|', 1);
    OPERATOR_PRECEDENCE.put('&', 2);
    OPERATOR_PRECEDENCE.put('!', 3);
    OPERATOR_PRECEDENCE.put(')', 4);
  }
}
