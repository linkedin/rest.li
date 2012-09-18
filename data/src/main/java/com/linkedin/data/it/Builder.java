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


import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.transforms.Transform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Builder that constructs {@link DataIterator}'s.
 * <p>
 * Example usage:
 * <code>
 *   DataIterator it = Builder.create(dataMap, schema, IterationOrder.PRE_ORDER)
 *                            .filterBy(Predicates.nameEquals("memberId"))
 *                            .dataIterator();
 *   DateElement element;
 *   while ((element = it.next) != null)
 *   {
 *     ...
 *   }
 *
 *   Builder.create(dataMap, schema, IterationOrder.POST_ORDER)
 *          .filterBy(Predicates.dataSchemaTypeEquals(DataSchema.Type.RECORD))
 *          .callback(new Callback() {
 *            public void callback(DataElement element)
 *            {
 *              ...
 *            }
 *          });
 * </code>
 */
public class Builder
{
  public static interface Callback
  {
    void callback(DataElement callback);
  }

  /**
   * Create a new Builder with the start Data object and its {@link DataSchema}.
   *
   * @param object provides the start Data object.
   * @param schema provides the {@link DataSchema} of the start Data object.
   * @return a {@link Builder}.
   */
  private static Builder create(Object object, DataSchema schema)
  {
    return create(object, schema, IterationOrder.PRE_ORDER);
  }

  /**
   * Create a new Builder with the start Data object and its {@link DataSchema}.
   *
   * @param object provides the start Data object.
   * @param schema provides the {@link DataSchema} of the start Data object.
   * @param order provides the iteration order.
   * @return a {@link Builder}.
   */
  public static Builder create(Object object, DataSchema schema, IterationOrder order)
  {
    return new Builder(new SimpleDataElement(object, schema), order);
  }

  /**
   * Create a new Builder with the start Data object obtained from
   * the provided {@link DataElement}.
   *
   * @param element that provides the start Data object.
   * @return a {@link Builder}.
   */
  private static Builder create(DataElement element)
  {
    return create(element, IterationOrder.PRE_ORDER);
  }

  /**
   * Create a new Builder with the start Data object
   * and {@link DataSchema} obtained from
   * the provided {@link DataElement}.
   *
   * @param element that provides the start Data object.
   * @param order provides the iteration order.
   * @return a {@link Builder}.
   */
  public static Builder create(DataElement element, IterationOrder order)
  {
    return new Builder(element, order);
  }

  /**
   * Create a new Builder with the start Data object obtained from
   * the provided {@link DataTemplate}.
   *
   * @param template that provides the start Data object.
   * @return a {@link Builder}.
   */
  private static Builder create(DataTemplate<? extends Object> template)
  {
    return create(template.data(), template.schema(), IterationOrder.PRE_ORDER);
  }

  /**
   * Create a new Builder with the start Data object
   * and {@link DataSchema} obtained from
   * the provided {@link DataTemplate}.
   *
   * @param template that provides the start Data object.
   * @param order provides the iteration order.
   * @return a {@link Builder}.
   */
  public static Builder create(DataTemplate<? extends Object> template, IterationOrder order)
  {
    return create(template.data(), template.schema(), order);
  }
  

  protected Builder(DataElement element, IterationOrder order)
  {
    _element = element;
    _order = order;
  }

  public Builder filterBy(Predicate predicate)
  {
    _predicates.add(predicate);
    return this;
  }
  
  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and accumulates Data objects returned by the iterator into the provided collection.
   * This method mutates the provided collection.
   * 
   * @param accumulator provides the collection that the accumulated data objects are added to.
   * @return the passed in collection, with the Data objects returned by the iterator added into it.
   * @see ValueAccumulator
   */
  public Collection<Object> accumulateValues(Collection<Object> accumulator)
  {
    return ValueAccumulator.accumulateValues(dataIterator(), accumulator);
  }
  
  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and accumulates Data objects returned by the iterator as a collection.
   * 
   * @return the Data objects accumulated from the {@link DataIterator}.
   * @see ValueAccumulator
   */
  public Collection<Object> accumulateValues()
  {
    return ValueAccumulator.accumulateValues(dataIterator());
  }

  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and counts the number of {Link DataElement}s returned by the iterator.
   * 
   * @return the count of Data objects.
   * @see Counter
   */
  public int count()
  {
    return Counter.count(dataIterator());
  }

  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and removes the Data objects returned by the iterator.
   * This method mutates the Data object and it's descendants.
   * This method does not change the start Data object referenced by the Builder.
   *
   * @return null if the input Data object is removed, else the root Data object.
   * @see Remover
   */
  public Object remove()
  {
    return Remover.remove(_element.getValue(), dataIterator());
  }

  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and transforms the Data objects returned by the iterator.
   * This method mutates the Data object and it's descendants.
   * This method does not change the start Data object referenced by the Builder.
   * 
   * @param transform provides the transformation that will be used to replace Data objects.
   * @return the replacement if the root object was replaced by a transformation, else the root object with the transformations applied.
   * @see Transformer
   */
  public Object transform(Transform<Object,Object> transform)
  {
    return Transformer.transform(_element.getValue(), dataIterator(), transform);
  }
  
  /**
   * Obtains a {@link DataIterator} from the {@link Builder} and replaces the Data objects returned by the iterator.
   * This method mutates the Data object and it's descendants.
   * This method does not change the start Data object referenced by the Builder.
   * 
   * @param value provides the object that Data objects are replaced with.
   * @return the replacement if the root object was replaced, else the root object with the replacements applied.
   * @see Transformer
   */
  public Object replace(Object value)
  {
    return Transformer.replace(_element.getValue(), dataIterator(), value);
  }

  public DataIterator dataIterator()
  {
    DataIterator it = new ObjectIterator(_element, _order);
    return _predicates.isEmpty() ? it : new FilterIterator(it, new AndPredicate(_predicates));
  }

  public void iterate(Callback callback)
  {
    DataIterator it = dataIterator();
    DataElement element;
    while ((element = it.next()) != null)
    {
      callback.callback(element);
    }
  }

  private List<Predicate> _predicates = new ArrayList<Predicate>();
  private DataElement _element;
  private IterationOrder _order;
}
