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

package com.linkedin.restli.internal.common;

import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.FilterConstants;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Class with implementation of helper methods to serialize/deserialize mask to/from URI
 * parameter.
 *
 * @author Josh Walker
 * @author jodzga
 */
public class URIMaskUtil
{

  /**
   * Generate a serialized string for the input {@link MaskTree}. The returned string is not URL encoded and must be
   * encoded elsewhere before using this in the request URI.
   *
   * @param maskTree the {@link MaskTree} to serialize
   * @return a String
   */
  public static String encodeMaskForURI(MaskTree maskTree)
  {
    return URIMaskUtil.encodeMaskForURI(maskTree.getDataMap());
  }

  /**
   * Generate a serialized string for the input {@link MaskTree}. The returned string is not URL encoded and must be
   * encoded elsewhere before using this in the request URI.
   *
   * @param simplifiedMask {@link DataMap} representation of the mask to serialize
   * @return a String
   */
  public static String encodeMaskForURI(DataMap simplifiedMask)
  {
    StringBuilder result = new StringBuilder();
    URIMaskUtil.encodeMaskForURIImpl(result, simplifiedMask, false);
    return result.toString();
  }

  private static void encodeMaskForURIImpl(StringBuilder result, DataMap simplifiedMask, boolean parenthesize)
  {
    if (parenthesize)
    {
      result.append(":(");
    }
    boolean delimit = false;
    for (Map.Entry<String, Object> entry : simplifiedMask.entrySet())
    {
      if (delimit)
      {
        result.append(',');
      }
      delimit = true;

      if ((FilterConstants.START.equals(entry.getKey()) || FilterConstants.COUNT.equals(entry.getKey())) &&
          entry.getValue() instanceof Integer)
      {
        result.append(entry.getKey());
        result.append(':').append(entry.getValue());
      }
      else if (entry.getValue().equals(MaskOperation.POSITIVE_MASK_OP.getRepresentation()))
      {
        result.append(entry.getKey());
      }
      else if (entry.getValue()
                    .equals(MaskOperation.NEGATIVE_MASK_OP.getRepresentation()))
      {
        result.append('-');
        result.append(entry.getKey());
      }
      else
      {
        result.append(entry.getKey());
        encodeMaskForURIImpl(result, (DataMap) entry.getValue(), true);
      }
    }
    if (parenthesize)
    {
      result.append(')');
    }
  }

  /**
   * Return a {@link MaskTree} that is deserialized from the input projection mask string used in URI parameter. The
   * input projection string must have been URL decoded if the projection was part of a request URI.
   *
   * @param toparse StringBuilder containing a string representation of an encoded MaskTree
   * @return a MaskTree
   * @throws IllegalMaskException if syntax in the input is malformed
   * @deprecated use {@link #decodeMaskUriFormat(String)} instead.
   */
  @Deprecated
  public static MaskTree decodeMaskUriFormat(StringBuilder toparse) throws IllegalMaskException
  {
    return decodeMaskUriFormat(toparse.toString());
  }

  /**
   * Return a {@link MaskTree} that is deserialized from the input projection mask string used in URI parameter. The
   * input projection string must have been URL decoded if the projection was part of a request URI.
   *
   * @param toparse String representing an encoded MaskTree
   * @return a MaskTree
   * @throws IllegalMaskException if syntax in the input is malformed
   */
  public static MaskTree decodeMaskUriFormat(String toparse) throws IllegalMaskException
  {
    ParseState state = ParseState.PARSE_FIELDS;
    int index = 0;
    DataMap result = new DataMap();
    Deque<DataMap> stack = new ArrayDeque<>();
    stack.addLast(result);
    StringBuilder field = new StringBuilder();

    while (index < toparse.length())
    {
      switch (state)
      {
      case TRAVERSE:
        if (toparse.charAt(index) != ',')
        {
          throw new IllegalStateException("Internal Error parsing mask: unexpected parse buffer '"
              + toparse.substring(index) + "' while traversing");
        }
        index++;
        state = ParseState.PARSE_FIELDS;
        break;
      case DESCEND:
        if (toparse.charAt(index) != ':' || toparse.charAt(index + 1) != '(')
        {
          throw new IllegalStateException("Internal Error parsing mask: unexpected parse buffer '"
              + toparse.substring(index) + "' while descending");
        }
        index += 2;
        state = ParseState.PARSE_FIELDS;
        break;
      case PARSE_FIELDS:
        Integer maskValue;
        if (toparse.charAt(index) == '-')
        {
          maskValue = MaskOperation.NEGATIVE_MASK_OP.getRepresentation();
          index++;
        }
        else
        {
          maskValue = MaskOperation.POSITIVE_MASK_OP.getRepresentation();
        }

        int nextToken = -1;
        field.setLength(0);
        int fieldIndex = index;
        for (; fieldIndex < toparse.length(); ++fieldIndex)
        {
          char c = toparse.charAt(fieldIndex);
          switch (c)
          {
          case ',':
            state = ParseState.TRAVERSE;
            nextToken = fieldIndex;
            break;
          case ':':
            if ((fieldIndex + 1) >= toparse.length())
            {
              throw new IllegalMaskException("Malformed mask syntax: unexpected end of buffer after ':'");
            }
            if ((field.length() == FilterConstants.START.length() && field.indexOf(FilterConstants.START) == 0)
                || (field.length() == FilterConstants.COUNT.length() && field.indexOf(FilterConstants.COUNT) == 0))
            {
              if (!Character.isDigit(toparse.charAt(fieldIndex + 1)))
              {
                throw new IllegalMaskException("Malformed mask syntax: unexpected range value");
              }

              fieldIndex++;

              // Aggressively consume the numerical value for the range parameter as this is a special case.
              int rangeValue = 0;
              while (fieldIndex < toparse.length() && nextToken == -1)
              {
                char ch = toparse.charAt(fieldIndex);
                switch (ch)
                {
                  case ',':
                    state = ParseState.TRAVERSE;
                    nextToken = fieldIndex;
                    break;
                  case ')':
                    state = ParseState.ASCEND;
                    nextToken = fieldIndex;
                    break;
                  default:
                    if (Character.isDigit(ch))
                    {
                      rangeValue = rangeValue * 10 + (ch - '0');
                    }
                    else
                    {
                      throw new IllegalMaskException("Malformed mask syntax: unexpected range value");
                    }
                    fieldIndex++;
                    break;
                }
              }

              // Set the mask value to the range value specified for the parameter
              maskValue = rangeValue;
            }
            else
            {
              if (toparse.charAt(fieldIndex + 1) != '(')
              {
                throw new IllegalMaskException("Malformed mask syntax: expected '(' token");
              }

              state = ParseState.DESCEND;
              nextToken = fieldIndex;
            }
            break;
          case ')':
            state = ParseState.ASCEND;
            nextToken = fieldIndex;
            break;
          default:
            if (!Character.isWhitespace(c))
            {
              field.append(c);
            }
            break;
          }
          if (nextToken != -1)
          {
            break;
          }
        }
        if (toparse.length() != fieldIndex)
        {
          if (nextToken == -1)
          {
            throw new IllegalMaskException("Malformed mask syntax: expected closing token");
          }
          index = nextToken;
        }
        else
        {
          index = toparse.length();
        }
        if (state == ParseState.DESCEND)
        {
          if (field.length() == 0)
          {
            throw new IllegalMaskException("Malformed mask syntax: empty parent field name");
          }
          DataMap subTree = new DataMap();
          stack.peekLast().put(field.toString(), subTree);
          stack.addLast(subTree);
        }
        else if (field.length() != 0)
        {
          stack.peekLast().put(field.toString(), maskValue);
        }
        break;
      case ASCEND:
        if (toparse.charAt(index) != ')')
        {
          throw new IllegalStateException("Internal Error parsing mask: unexpected parse buffer '"
              + toparse.substring(index) + "' while ascending");
        }
        if (stack.isEmpty())
        {
          throw new IllegalMaskException("Malformed mask syntax: unexpected ')' token");
        }
        index++;
        stack.removeLast();
        state = ParseState.PARSE_FIELDS;
        break;
      }
    }
    if (stack.size() != 1)
    {
      throw new IllegalMaskException("Malformed mask syntax: unmatched nesting");
    }
    result = stack.removeLast();
    return new MaskTree(result);
  }

  private enum ParseState
  {
    DESCEND, PARSE_FIELDS, ASCEND, TRAVERSE
  }

}
