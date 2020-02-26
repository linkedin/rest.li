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

import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.message.MessageUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Expanded from {@link com.linkedin.data.schema.DataSchemaTraverse}
 * There are two main differences:
 * (1) This new traverser provides rich context that passed to the visitors when visiting data schemas
 * (2) It will also traverse to the schemas even the schema has been seen before. (But there are mechanisms to prevent cycles)
 */
public class DataSchemaRichContextTraverser
{
  /**
   * Use this {@link IdentityHashMap} to prevent traversing through a cycle, when traversing along parents to child.
   * for the example below, Rcd's f1 field also points to a Rcd which formed a cycle and we should stop visiting it.
   * <pre>
   * record Rcd{
   *   f1: Rcd
   * }
   * </pre>
   *
   * But note that this HashMap will not prevent traversing a seen data schema that is not from its ancestor.
   * e.g.
   * <pre>
   *   record Rcd {
   *     f1: Rcd2
   *     f2: Rcd2
   *   }
   * </pre>
   * In this case, both f1 and f2 will be visited.
   *
   * This HashMap help the traverser to recognize when encountering "Rcd" for the second time if it forms a cycle
   */
  private final IdentityHashMap<DataSchema, Boolean> _seenAncestorsDataSchema = new IdentityHashMap<>();
  private SchemaVisitor _schemaVisitor;
  /**
   * Store the original data schema that has been passed.
   * The {@link DataSchemaRichContextTraverser} should not modify this DataSchema during the traversal
   * which could ensure the correctness of the traversal
   *
   */
  private DataSchema _originalTopLevelSchemaUnderTraversal;

  public DataSchemaRichContextTraverser(SchemaVisitor schemaVisitor)
  {
    _schemaVisitor = schemaVisitor;
  }

  public void traverse(DataSchema schema)
  {
    _originalTopLevelSchemaUnderTraversal = schema;
    TraverserContext traverserContext = new TraverserContext();
    traverserContext.setOriginalTopLevelSchema(_originalTopLevelSchemaUnderTraversal);
    traverserContext.setCurrentSchema(schema);
    traverserContext.setVisitorContext(_schemaVisitor.getInitialVisitorContext());
    doRecursiveTraversal(traverserContext);
  }

  private void doRecursiveTraversal(TraverserContext context)
  {

    // Add full name to the context's TraversePath
    DataSchema schema = context.getCurrentSchema();
    ArrayDeque<String> path = context.getTraversePath();
    path.add(schema.getUnionMemberKey());

    // visitors
    _schemaVisitor.callbackOnContext(context, DataSchemaTraverse.Order.PRE_ORDER);

    /**
     * By default {@link DataSchemaRichContextTraverser} will only decide whether or not keep traversing based on whether the new
     * data schema has been seen.
     *
     * But the {@link SchemaVisitor} has the chance to override this control by setting {@link TraverserContext#_shouldContinue}
     * If this variable set to be {@link Boolean#TRUE}, the {@link DataSchemaRichContextTraverser} will traverse to next level (if applicable)
     * If this variable set to be {@link Boolean#FALSE}, the {@link DataSchemaRichContextTraverser} will stop traversing to next level
     * If this variable not set, the {@link DataSchemaRichContextTraverser} will decide whether or not to continue traversing based on whether
     * this data schema has been seen.
     */
    if (context.shouldContinue() == Boolean.TRUE ||
        !(context.shouldContinue() == Boolean.FALSE || _seenAncestorsDataSchema.containsKey(schema)))
    {
      _seenAncestorsDataSchema.put(schema, Boolean.TRUE);

      // Pass new context in every recursion
      TraverserContext nextContext = null;

      switch (schema.getType())
      {
        case TYPEREF:
          TyperefDataSchema typerefDataSchema = (TyperefDataSchema) schema;

          nextContext = context.getNextContext(DataSchemaConstants.REF_KEY, null, typerefDataSchema.getRef(),
                                               CurrentSchemaEntryMode.TYPEREF_REF);
          doRecursiveTraversal(nextContext);
          break;
        case MAP:
          // traverse key
          MapDataSchema mapDataSchema = (MapDataSchema) schema;

          nextContext = context.getNextContext(DataSchemaConstants.MAP_KEY_REF, DataSchemaConstants.MAP_KEY_REF,
                                               mapDataSchema.getKey(), CurrentSchemaEntryMode.MAP_KEY);
          doRecursiveTraversal(nextContext);

          // then traverse values
          nextContext = context.getNextContext(PathSpec.WILDCARD, PathSpec.WILDCARD, mapDataSchema.getValues(),
                                               CurrentSchemaEntryMode.MAP_VALUE);
          doRecursiveTraversal(nextContext);
          break;
        case ARRAY:
          ArrayDataSchema arrayDataSchema = (ArrayDataSchema) schema;

          nextContext = context.getNextContext(PathSpec.WILDCARD, PathSpec.WILDCARD, arrayDataSchema.getItems(),
                                               CurrentSchemaEntryMode.ARRAY_VALUE);
          doRecursiveTraversal(nextContext);
          break;
        case RECORD:
          RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
          for (RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            nextContext =
                context.getNextContext(field.getName(), field.getName(), field.getType(), CurrentSchemaEntryMode.FIELD);
            nextContext.setEnclosingField(field);
            doRecursiveTraversal(nextContext);
          }
          break;
        case UNION:
          UnionDataSchema unionDataSchema = (UnionDataSchema) schema;
          for (UnionDataSchema.Member member : unionDataSchema.getMembers())
          {
            nextContext =
                context.getNextContext(member.getUnionMemberKey(), member.getUnionMemberKey(), member.getType(),
                                       CurrentSchemaEntryMode.UNION_MEMBER);
            nextContext.setEnclosingUnionMember(member);
            doRecursiveTraversal(nextContext);
          }
          break;
        default:
          // will stop recursively traversing if the current schema is a leaf node.
          assert isLeafSchema(schema);
          break;
      }
      _seenAncestorsDataSchema.remove(schema);
    }
    _schemaVisitor.callbackOnContext(context, DataSchemaTraverse.Order.POST_ORDER);
  }

  /**
   * Enum to tell how the current schema is linked from its parentSchema as a child schema
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

  /**
   * Returns true if the dataSchema is a leaf node.
   *
   * a leaf DataSchema is a schema that doesn't have other types of DataSchema linked from it.
   * Below types are leaf DataSchemas
   * {@link com.linkedin.data.schema.PrimitiveDataSchema} ,
   * {@link com.linkedin.data.schema.EnumDataSchema} ,
   * {@link com.linkedin.data.schema.FixedDataSchema}
   *
   * Other dataSchema types, for example {@link com.linkedin.data.schema.TyperefDataSchema} could link to another DataSchema
   * so it is not a leaf DataSchema
   */
  public static boolean isLeafSchema(DataSchema dataSchema)
  {
    return (dataSchema instanceof PrimitiveDataSchema)
           || (dataSchema.getType() == DataSchema.Type.FIXED)
           || (dataSchema.getType() == DataSchema.Type.ENUM);
  }

  public DataSchema getOriginalTopLevelSchemaUnderTraversal()
  {
    return _originalTopLevelSchemaUnderTraversal;
  }

  /**
   * Interface for SchemaVisitor, which will be called by {@link DataSchemaRichContextTraverser}.
   */
  public interface SchemaVisitor
  {
    /**
     * The callback function that will be called by {@link DataSchemaRichContextTraverser} visiting the dataSchema under traversal.
     * This function will be called TWICE within {@link DataSchemaRichContextTraverser}, during two {@link DataSchemaTraverse.Order}s
     * {@link DataSchemaTraverse.Order#PRE_ORDER} and {@link DataSchemaTraverse.Order#POST_ORDER} respectively.
     *
     * @param context
     * @param order the order given by {@link DataSchemaRichContextTraverser} to tell whether this call happens during pre order or post order
     */
    void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order);

    /**
     * {@link SchemaVisitor} implements this method to return an initial {@link VisitorContext}
     * {@link VisitorContext} will be stored inside {@link DataSchemaRichContextTraverser.TraverserContext} and then
     * passed to {@link SchemaVisitor} during recursive traversal
     *
     * @return an initial {@link VisitorContext} that will be stored by {@link SchemaVisitor}
     *
     * @see VisitorContext
     */
    VisitorContext getInitialVisitorContext();

    /**
     * The visitor should store a {@link VisitorTraversalResult} which stores this visitor's traversal result.
     *
     * @return traversal result after the visitor traversed the schema
     */
    VisitorTraversalResult getVisitorTraversalResult();
  }

  /**
   * A context that is defined and handled by {@link SchemaVisitor}
   *
   * The {@link DataSchemaRichContextTraverser} will get the initial context and then
   * passing this as part of {@link DataSchemaRichContextTraverser.TraverserContext}
   *
   * {@link SchemaVisitor} implementations can store customized information that want to pass during recursive traversal here
   * similar to how {@link DataSchemaRichContextTraverser.TraverserContext} is used.
   *
   * @see DataSchemaRichContextTraverser.TraverserContext
   */
  public interface VisitorContext
  {
  }

  /**
   * The traversal result stores states of the traversal result for each visitor.
   * It should tell whether the traversal is successful and stores error messages if not
   *
   * There are two kinds of error messages
   * (1) An error message with {@link Message} type, it will be collected to the {@link Message} list and formatted and
   * outputted by the string builder.
   * (2) User can also directly add string literal messages and output them using the string builder.
   *
   * @see Message
   */
  public static class VisitorTraversalResult
  {

    boolean _isTraversalSuccessful = true;
    MessageList<Message> _messages = new MessageList<>();
    StringBuilder _messageBuilder = new StringBuilder();
    /**
     * The {@link SchemaVisitor} should not mutate the original {@link DataSchema} that {@link DataSchemaRichContextTraverser} is traversing,
     * instead it needs to construct a new one if it needs to update the original schema.
     * This is useful if the new updated {@link DataSchema} is needed for later reuse.
     * If no update on the original schema is needed, this variable should remain null.
     */
    DataSchema _constructedSchema = null;

    public DataSchema getConstructedSchema()
    {
      return _constructedSchema;
    }

    public void setConstructedSchema(DataSchema constructedSchema)
    {
      _constructedSchema = constructedSchema;
    }

    /**
     * Return whether there are errors detected during the traversal.
     * @return boolean to tell whether the traversal is successful or not
     */
    public boolean isTraversalSuccessful()
    {
      return _isTraversalSuccessful;
    }

    /**
     * private method for setting whether the traversal is successful.
     *
     * @param traversalSuccessful the boolean value to represent whether the traversal is successful
     *
     * @see #isTraversalSuccessful()
     */
    private void setTraversalSuccessful(boolean traversalSuccessful)
    {
      _isTraversalSuccessful = traversalSuccessful;
    }

    /**
     * Getter for messages lists
     * @return collection of messages gather during traversal
     */
    public Collection<Message> getMessages()
    {
      return _messages;
    }

    /**
     * Setter for message lists
     * @param messages
     */
    public void setMessages(MessageList<Message> messages)
    {
      _messages = messages;
      if (messages != null && messages.size() > 0)
      {
        setTraversalSuccessful(false);
      }
    }

    /**
     * Add a message to the message list and the string builder
     * @param message
     */
    public void addMessage(Message message)
    {
      _messages.add(message);
      MessageUtil.appendMessages(getMessageBuilder(), Arrays.asList(message));
      setTraversalSuccessful(false);
    }

    /**
     * Add a {@link Message} to the message list using constructor of the {@link Message}
     * and also add to the string builder
     *
     * @param path path to show in the message
     * @param format format of the message to show
     * @param args args for the format string
     *
     * @see Message
     */
    public void addMessage(ArrayDeque<String> path, String format, Object... args)
    {
      Message msg = new Message(path.toArray(), format, args);
      addMessage(msg);
    }

    /**
     * Add multiple {@link Message}s to the message list and the string builder
     * These message added shows same path
     *
     * @param path path of the location where the messages are added
     * @param messages the message to add to the message list
     *
     * @see Message
     */
    public void addMessages(ArrayDeque<String> path, Collection<? extends Message> messages)
    {
      List<Message> msgs = messages.stream()
                                   .map(msg -> new Message(path.toArray(), ((Message) msg).toString()))
                                   .collect(Collectors.toList());
      _messages.addAll(msgs);
      MessageUtil.appendMessages(getMessageBuilder(), msgs);
      setTraversalSuccessful(false);
    }

    public StringBuilder getMessageBuilder()
    {
      return _messageBuilder;
    }

    /**
     * Output the string builder content as a string
     *
     * @return a string output by the string builder
     */
    public String formatToErrorMessage()
    {
      return getMessageBuilder().toString();
    }
  }

  /**
   * Context defined by {@link DataSchemaRichContextTraverser} that will be updated and handled during traversal
   *
   * A new {@link TraverserContext} object will be created before entering child from parent.
   * In this way, we simulate {@link TraverserContext} as elements inside stack during recursive traversal.
   */
  static class TraverserContext
  {
    /**
     * Use this flag to control whether DataSchemaRichContextTraverser should continue to traverse from parent to child.
     * This variable can be set to null if want default behavior.
     */
    Boolean _shouldContinue = null;
    DataSchema _originalTopLevelSchema;
    DataSchema _parentSchema;
    DataSchema _currentSchema;
    /**
     * This traverse path is a very detailed path, and is same as the path used in {@link DataSchemaTraverse}
     * This path's every component corresponds to a move by traverser, and its components have TypeRef components and record name.
     * Example:
     * <pre>
     * record Test {
     *   f1: record Nested {
     *     f2: typeref TypeRef_Name=int
     *   }
     * }
     * </pre>
     * The traversePath to the f2 field would be as detailed as "/Test/f1/Nested/f2/TypeRef_Name/int"
     * Meanwhile its schema pathSpec is as simple as "/f1/f2"
     *
     */
    ArrayDeque<String> _traversePath = new ArrayDeque<>();
    /**
     * This is the path components corresponds to {@link PathSpec}, it would not have TypeRef component inside its component list, also it would only contain field's name
     */
    ArrayDeque<String> _schemaPathSpec = new ArrayDeque<>();
    /**
     * If the context is passing down from a {@link RecordDataSchema}, this attribute will be set with the enclosing
     * {@link RecordDataSchema.Field}
     */
    RecordDataSchema.Field _enclosingField;
    /**
     * If the context is passing down from a {@link UnionDataSchema}, this attribute will be set with the enclosing
     * {@link UnionDataSchema.Member}
     */
    UnionDataSchema.Member _enclosingUnionMember;
    /**
     * This attribute tells how {@link #_currentSchema} stored in the context is linked from its parentSchema
     * For example, if the {@link CurrentSchemaEntryMode} specify the {@link #_currentSchema} is an union member of parent Schema,
     * User can expect parentSchema is a {@link UnionDataSchema} and the {@link #_enclosingUnionMember} should be the
     * enclosing union member that stores the current schema.
     *
     * @see CurrentSchemaEntryMode
     */
    CurrentSchemaEntryMode _currentSchemaEntryMode;
    /**
     * SchemaAnnotationVisitors can set customized context
     * @see VisitorContext
     */
    VisitorContext _visitorContext;

    VisitorContext getVisitorContext()
    {
      return _visitorContext;
    }

    /**
     * Generate a new {@link TraverserContext} for next recursion in {@link #doRecursiveTraversal(TraverserContext)}
     *
     * @param nextTraversePathComponent pathComponent of the traverse path of the next dataSchema to be traversed
     * @param nextSchemaPathSpecComponent pathComponent of the schema path of the next dataSchema to be traversed
     * @param nextSchema the next dataSchema to be traversed
     * @param nextSchemaEntryMode how next dataSchema is linked from current dataSchema.
     * @return a new {@link TraverserContext} generated for next recursion
     */
    TraverserContext getNextContext(String nextTraversePathComponent, String nextSchemaPathSpecComponent,
                                    DataSchema nextSchema, CurrentSchemaEntryMode nextSchemaEntryMode)
    {
      TraverserContext nextContext = new TraverserContext();
      nextContext.setOriginalTopLevelSchema(this.getOriginalTopLevelSchema());
      nextContext.setParentSchema(this.getCurrentSchema());
      nextContext.setSchemaPathSpec(new ArrayDeque<>(this.getSchemaPathSpec()));
      nextContext.setVisitorContext(this.getVisitorContext());
      nextContext.setEnclosingField(this.getEnclosingField());
      nextContext.setEnclosingUnionMember(this.getEnclosingUnionMember());

      // Need to make a copy so if it is modified in next recursion
      // it won't affect this recursion
      nextContext.setTraversePath(new ArrayDeque<>(this.getTraversePath()));
      nextContext.getTraversePath().add(nextTraversePathComponent);
      // Same as traversePath, we need to make a copy.
      nextContext.setSchemaPathSpec(new ArrayDeque<>(this.getSchemaPathSpec()));
      // SchemaPathSpecComponent could be null if nextSchema is a TypeRefDataSchema
      if (nextSchemaPathSpecComponent != null)
      {
        nextContext.getSchemaPathSpec().add(nextSchemaPathSpecComponent);
      }
      nextContext.setCurrentSchema(nextSchema);
      nextContext.setCurrentSchemaEntryMode(nextSchemaEntryMode);
      return nextContext;
    }

    public Boolean shouldContinue()
    {
      return _shouldContinue;
    }

    public void setShouldContinue(Boolean shouldContinue)
    {
      this._shouldContinue = shouldContinue;
    }

    void setVisitorContext(VisitorContext visitorContext)
    {
      _visitorContext = visitorContext;
    }

    ArrayDeque<String> getSchemaPathSpec()
    {
      return _schemaPathSpec;
    }

    void setSchemaPathSpec(ArrayDeque<String> schemaPathSpec)
    {
      _schemaPathSpec = schemaPathSpec;
    }

    DataSchema getCurrentSchema()
    {
      return _currentSchema;
    }

    void setCurrentSchema(DataSchema currentSchema)
    {
      _currentSchema = currentSchema;
    }

    ArrayDeque<String> getTraversePath()
    {
      return _traversePath;
    }

    void setTraversePath(ArrayDeque<String> traversePath)
    {
      this._traversePath = traversePath;
    }

    DataSchema getParentSchema()
    {
      return _parentSchema;
    }

    void setParentSchema(DataSchema parentSchema)
    {
      _parentSchema = parentSchema;
    }

    RecordDataSchema.Field getEnclosingField()
    {
      return _enclosingField;
    }

    void setEnclosingField(RecordDataSchema.Field enclosingField)
    {
      _enclosingField = enclosingField;
    }

    UnionDataSchema.Member getEnclosingUnionMember()
    {
      return _enclosingUnionMember;
    }

    void setEnclosingUnionMember(UnionDataSchema.Member enclosingUnionMember)
    {
      _enclosingUnionMember = enclosingUnionMember;
    }

    CurrentSchemaEntryMode getCurrentSchemaEntryMode()
    {
      return _currentSchemaEntryMode;
    }

    void setCurrentSchemaEntryMode(CurrentSchemaEntryMode currentSchemaEntryMode)
    {
      _currentSchemaEntryMode = currentSchemaEntryMode;
    }

    public DataSchema getOriginalTopLevelSchema()
    {
      return _originalTopLevelSchema;
    }

    public void setOriginalTopLevelSchema(DataSchema originalTopLevelSchema)
    {
      _originalTopLevelSchema = originalTopLevelSchema;
    }

  }
}
