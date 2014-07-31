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

/* $Id$ */
package com.linkedin.r2.transport.common;

import java.util.Map;

/**
 * Helper class which sets an outbound wire attribute (header) to indicate the message
 * type.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class MessageType
{
  private static final String REQUEST_TYPE_HEADER = "MsgType";

  public static enum Type
  {
    REST
  }

  /**
   * Inspect the wire attributes to determine the message type.
   *
   * @param wireAttrs wire attributes to inspect for message type header.
   * @param defaultType default message type to be returned if it cannot be determined
   *          from the wire attributes.
   * @return message type as specified in the wire attributes, or defaultType if the wire
   *         attributes do not specify a type.
   */
  public static Type getMessageType(Map<String, String> wireAttrs, Type defaultType)
  {
    String typeName = wireAttrs.get(REQUEST_TYPE_HEADER);
    return typeName == null ? defaultType : Type.valueOf(typeName);
  }

  /**
   * Set an indicator into the wire attributes to specify the message type.
   *
   * @param type message type to be set.
   * @param wireAttrs wire attributes in which the header should be inserted.
   */
  public static void setMessageType(Type type, Map<String, String> wireAttrs)
  {
    wireAttrs.put(REQUEST_TYPE_HEADER, type.name());
  }

  private MessageType()
  {
  }
}
