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

package com.linkedin.data.template;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asList;
import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.noCommonDataComplex;
import static org.testng.Assert.*;


/**
 * Unit tests for array {@link DataTemplate}'s.
 *
 * @author slim
 */
public class TestArrayTemplate
{
  public static <ArrayTemplate extends AbstractArrayTemplate<E>, E>
  void testArray(Class<ArrayTemplate> templateClass,
                 ArrayDataSchema schema,
                 List<E> input,
                 List<E> adds)
  {
    try
    {
      // constructors and addAll
      ArrayTemplate array1 = templateClass.getDeclaredConstructor().newInstance();
      array1.addAll(input);
      assertEquals(input, array1);

      /*
      Constructor[] constructors = templateClass.getConstructors();
      for (Constructor c : constructors)
      {
        out.println(c);
      }
      */
      try
      {
        int size = input.size();

        // constructor(int capacity)
        Constructor<ArrayTemplate> capacityConstructor = templateClass.getConstructor(int.class);
        ArrayTemplate array = capacityConstructor.newInstance(input.size());
        assertEquals(array, Collections.emptyList());
        array.addAll(input);
        assertEquals(input, array);
        array.clear();
        assertEquals(size, input.size());

        // constructor(Collection<E>)
        Constructor<ArrayTemplate> collectionConstructor = templateClass.getConstructor(Collection.class);
        array = collectionConstructor.newInstance(input);
        assertEquals(input, array);
        array.clear();
        assertEquals(size, input.size());

        // constructor(DataList)
        Constructor<ArrayTemplate> dataListConstructor = templateClass.getConstructor(DataList.class);
        array = dataListConstructor.newInstance(array1.data());
        assertEquals(array1, array);
        assertEquals(input, array);
        array.clear();
        assertEquals(array1, array);
      }
      catch (Exception e)
      {
        assertSame(e, null);
      }

      // test wrapping
      array1.clear();
      array1.addAll(input);
      DataList dataList2 = new DataList();
      ArrayTemplate array2 = DataTemplateUtil.wrap(dataList2, schema, templateClass); // with schema arg
      for (E e : input)
      {
        if (e instanceof DataTemplate)
        {
          dataList2.add(((DataTemplate<?>) e).data());
        }
        else if (e instanceof Enum)
        {
          dataList2.add(e.toString());
        }
        else
        {
          dataList2.add(e);
        }
      }
      assertEquals(array1, array2);
      ArrayTemplate array2a = DataTemplateUtil.wrap(dataList2, templateClass); // without schema arg
      assertEquals(array1, array2a);
      assertSame(array2.data(), array2a.data());

      // schema()
      ArrayDataSchema schema1 = array1.schema();
      assertTrue(schema1 != null);
      assertEquals(schema1.getType(), DataSchema.Type.ARRAY);
      assertEquals(schema1, schema);

      // add(E element), get(int index)
      ArrayTemplate array3 = templateClass.getDeclaredConstructor().newInstance();
      for (int i = 0; i < adds.size(); ++i)
      {
        E value = adds.get(i);
        assertTrue(array3.add(value));
        Object getValue = array3.get(i);
        assertEquals(array3.get(i), value);
        assertSame(array3.get(i), getValue);
        assertTrue(array3.toString().contains(value.toString()));
      }
      assertEquals(array3, adds);

      // add(int index, E element), get(int index)
      ArrayTemplate array4 = templateClass.getDeclaredConstructor().newInstance();
      for (int i = 0; i < adds.size(); ++i)
      {
        E value = adds.get(adds.size() - i - 1);
        array4.add(0, value);
        Object getValue = array4.get(0);
        assertEquals(array4.get(0), value);
        assertSame(array4.get(0), getValue);
      }
      assertEquals(array4, adds);

      // clear(), isEmpty(), size()
      assertEquals(array4.size(), adds.size());
      assertFalse(array4.isEmpty());
      array4.clear();
      assertTrue(array4.isEmpty());
      assertEquals(array4.size(), 0);

      // equals()
      array4.clear();
      array4.addAll(input);
      assertTrue(array4.equals(array4));
      assertTrue(array4.equals(input));
      assertFalse(array4.equals(null));
      assertFalse(array4.equals(adds));
      for (int i = 0; i <= input.size(); ++i)
      {
        List<E> subList = input.subList(0, i);
        ArrayTemplate a = templateClass.getDeclaredConstructor().newInstance();
        a.addAll(subList);
        if (i == input.size())
        {
          assertTrue(array4.equals(subList));
          assertTrue(array4.equals(a));
        }
        else
        {
          assertFalse(array4.equals(subList));
          assertFalse(array4.equals(a));
        }
      }

      // hashcode()
      ArrayTemplate array5 = templateClass.getDeclaredConstructor().newInstance();
      array5.addAll(input);
      assertEquals(array5.hashCode(), array5.data().hashCode());
      array5.addAll(adds);
      assertEquals(array5.hashCode(), array5.data().hashCode());
      array5.clear();
      int lastHash = 0;
      for (int i = 0; i < input.size(); ++i)
      {
        array5.add(input.get(i));
        int newHash = array5.hashCode();
        if (i > 0)
        {
          assertFalse(newHash == lastHash);
        }
        lastHash = newHash;
      }

      // indexOf(Object o), lastIndexOf(Object o)
      ArrayTemplate array6 = templateClass.getDeclaredConstructor().newInstance();
      array6.addAll(adds);
      for (E e : adds)
      {
        assertEquals(array6.indexOf(e), adds.indexOf(e));
        assertEquals(array6.lastIndexOf(e), adds.lastIndexOf(e));
      }

      // remove(int index), subList(int fromIndex, int toIndex)
      ArrayTemplate array7 = templateClass.getDeclaredConstructor().newInstance();
      array7.addAll(input);
      ArrayTemplate array8 = templateClass.getDeclaredConstructor().newInstance();
      array8.addAll(input);
      for (int i = 0; i < input.size(); ++i)
      {
        array7.remove(0);
        assertEquals(array7, input.subList(i + 1, input.size()));
        assertEquals(array7, array8.subList(i + 1, input.size()));
      }

      // removeRange(int fromIndex, int toIndex), subList(int fromIndex, int toIndex)
      for (int from = 0; from < input.size(); ++from)
      {
        for (int to = from + 1; to <= input.size(); ++to)
        {
          ArrayTemplate arrayRemove = templateClass.getDeclaredConstructor().newInstance();
          arrayRemove.addAll(input);
          InternalList<E> reference = new InternalList<>(input);
          arrayRemove.removeRange(from, to);
          reference.removeRange(from, to);
          assertEquals(reference, arrayRemove);
        }
      }

      // set(int index, E element)
      ArrayTemplate array9 = templateClass.getDeclaredConstructor().newInstance();
      array9.addAll(input);
      InternalList<E> reference9 = new InternalList<>(input);
      for (int i = 0; i < input.size() / 2; ++i)
      {
        int k = input.size() - i - 1;
        E lo = array9.get(i);
        E hi = array9.get(k);
        E hiPrev = array9.set(k, lo);
        E loPrev = array9.set(i, hi);
        E refHiPrev = reference9.set(k, lo);
        E refLoPrev = reference9.set(i, hi);
        assertEquals(hiPrev, refHiPrev);
        assertEquals(loPrev, refLoPrev);
        assertEquals(array9.get(i), reference9.get(i));
        assertEquals(array9.get(k), reference9.get(k));
      }

      // clone and copy return types
      TestDataTemplateUtil.assertCloneAndCopyReturnType(templateClass);

      // clone
      Exception exc = null;
      ArrayTemplate array10 = templateClass.getDeclaredConstructor().newInstance();
      array10.addAll(input);
      try
      {
        @SuppressWarnings("unchecked")
        ArrayTemplate array10Clone = (ArrayTemplate) array10.clone();
        assertTrue(array10Clone.getClass() == templateClass);
        assertEquals(array10Clone, array10);
        assertTrue(array10Clone != array10);
        for (int i = 0; i < array10.size(); i++)
        {
          assertSame(array10Clone.data().get(i), array10.data().get(i));
        }
        array10Clone.remove(0);
        assertEquals(array10Clone.size(), array10.size() - 1);
        assertFalse(array10Clone.equals(array10));
        assertTrue(array10.containsAll(array10Clone));
        array10.remove(0);
        assertEquals(array10Clone, array10);
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assert(exc == null);

      // copy
      ArrayTemplate array10a = templateClass.getDeclaredConstructor().newInstance();
      array10a.addAll(input);

      try
      {
        @SuppressWarnings("unchecked")
        ArrayTemplate array10aCopy = (ArrayTemplate) array10a.copy();
        assertTrue(array10aCopy.getClass() == templateClass);
        assertEquals(array10a, array10aCopy);
        boolean hasComplex = false;
        for (int i = 0; i < array10a.size(); i++)
        {
          if (array10a.data().get(i) instanceof DataComplex)
          {
            assertNotSame(array10aCopy.data().get(i), array10a.data().get(i));
            hasComplex = true;
          }
        }
        assertTrue(DataTemplate.class.isAssignableFrom(array10a._elementClass) == false || hasComplex);
        assertTrue(noCommonDataComplex(array10a.data(), array10aCopy.data()));
        boolean mutated = false;
        for (Object items : array10aCopy.data())
        {
          mutated |= TestUtil.mutateChild(items);
        }
        assertEquals(mutated, hasComplex);
        if (mutated)
        {
          assertNotEquals(array10aCopy, array10a);
        }
        else
        {
          assertEquals(array10aCopy, array10a);
          array10aCopy.remove(0);
          assertNotEquals(array10aCopy, array10a);
        }
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assert(exc == null);

      // contains
      for (int i = 0; i < input.size(); ++i)
      {
        ArrayTemplate array = templateClass.getDeclaredConstructor().newInstance();
        E v = input.get(i);
        array.add(v);
        for (int k = 0; k < input.size(); ++k)
        {
          if (k == i)
            assertTrue(array.contains(v));
          else
            assertFalse(array.contains(input.get(k)));
        }
      }

      // containsAll
      ArrayTemplate arrayContainsAll = templateClass.getDeclaredConstructor().newInstance();
      arrayContainsAll.addAll(input);
      arrayContainsAll.addAll(adds);
      InternalList<E> referenceContainsAll = new InternalList<>(input);
      referenceContainsAll.addAll(adds);
      for (int from = 0; from < arrayContainsAll.size(); ++from)
      {
        for (int to = from + 1; to <= arrayContainsAll.size(); ++to)
        {
          boolean testResult = arrayContainsAll.containsAll(referenceContainsAll.subList(from, to));
          boolean referenceResult = referenceContainsAll.containsAll(referenceContainsAll.subList(from, to));
          assertEquals(testResult, referenceResult);
          assertTrue(testResult);
          assertTrue(referenceResult);
        }
        boolean testResult2 = arrayContainsAll.subList(from, arrayContainsAll.size()).containsAll(referenceContainsAll);
        boolean referenceResult2 = referenceContainsAll.subList(from, arrayContainsAll.size()).containsAll(referenceContainsAll);
        // out.println("from " + from + " test " + testResult2 + " ref " + referenceResult2);
        assertEquals(testResult2, referenceResult2);
      }

      // removeAll
      InternalList<E> referenceListRemoveAll = new InternalList<>(input);
      referenceListRemoveAll.addAll(adds);
      for (int from = 0; from < referenceListRemoveAll.size(); ++from)
      {
        for (int to = from + 1; to <= referenceListRemoveAll.size(); ++to)
        {
          ArrayTemplate arrayRemoveAll = templateClass.getDeclaredConstructor().newInstance();
          arrayRemoveAll.addAll(referenceListRemoveAll);
          InternalList<E> referenceRemoveAll = new InternalList<>(referenceListRemoveAll);

          boolean testResult = arrayRemoveAll.removeAll(referenceListRemoveAll.subList(from, to));
          boolean referenceResult = referenceRemoveAll.removeAll(referenceListRemoveAll.subList(from, to));
          // out.println("from " + from + " to " + to + " test " + testResult + " " + arrayRemoveAll + " ref " + referenceResult + " " + referenceRemoveAll);
          assertEquals(arrayRemoveAll, referenceRemoveAll);
          assertEquals(testResult, referenceResult);
          assertTrue(testResult);
          assertTrue(referenceResult);
        }
      }

      // retainAll
      InternalList<E> referenceListRetainAll = new InternalList<>(input);
      referenceListRetainAll.addAll(adds);
      for (int from = 0; from < referenceListRetainAll.size(); ++from)
      {
        for (int to = from + 1; to <= referenceListRetainAll.size(); ++to)
        {
          ArrayTemplate arrayRetainAll = templateClass.getDeclaredConstructor().newInstance();
          arrayRetainAll.addAll(referenceListRetainAll);
          InternalList<E> referenceRetainAll = new InternalList<>(referenceListRetainAll);

          boolean testResult = arrayRetainAll.removeAll(referenceListRetainAll.subList(from, to));
          boolean referenceResult = referenceRetainAll.removeAll(referenceListRetainAll.subList(from, to));
          // out.println("from " + from + " to " + to + " test " + testResult + " " + arrayRetainAll + " ref " + referenceResult + " " + referenceRetainAll);
          assertEquals(arrayRetainAll, referenceRetainAll);
          assertEquals(testResult, referenceResult);
          assertTrue(testResult);
          assertTrue(referenceResult);
        }
      }

      // Iterator
      ArrayTemplate arrayIt = templateClass.getDeclaredConstructor().newInstance();
      arrayIt.addAll(input);
      arrayIt.addAll(adds);
      for (Iterator<E> it = arrayIt.iterator(); it.hasNext(); )
      {
        it.next();
        it.remove();
      }
      assertTrue(arrayIt.isEmpty());

      // ListIterator hasNext, hasPrevious, next, previous
      ArrayTemplate arrayListIt = templateClass.getDeclaredConstructor().newInstance();
      arrayListIt.addAll(input);
      arrayListIt.addAll(adds);
      for (int i = 0; i <= arrayListIt.size(); ++i)
      {
        ListIterator<E> it = arrayListIt.listIterator(i);
        if (i > 0)
        {
          int save = it.nextIndex();
          assertTrue(it.hasPrevious());
          assertEquals(it.previous(), arrayListIt.get(i - 1));
          it.next();
          assertEquals(it.nextIndex(), save);
        }
        else
        {
          assertFalse(it.hasPrevious());
        }
        if (i < arrayListIt.size())
        {
          int save = it.previousIndex();
          assertTrue(it.hasNext());
          assertEquals(it.next(), arrayListIt.get(i));
          it.previous();
          assertEquals(it.previousIndex(), save);
        }
        else
        {
          assertFalse(it.hasNext());
        }
        assertEquals(it.nextIndex(), i);
        assertEquals(it.previousIndex(), i - 1);
      }

      // ListIterator remove
      for (ListIterator<E> it = arrayListIt.listIterator(); it.hasNext(); )
      {
        it.next();
        it.remove();
      }
      assertTrue(arrayListIt.isEmpty());

      // ListIterator add
      arrayListIt.clear();
      {
        ListIterator<E> it = arrayListIt.listIterator();
        for (E e : adds)
        {
          it.add(e);
        }
      }
      assertEquals(arrayListIt, adds);

      // ListIterator set
      for (int i = 0; i < adds.size(); ++i)
      {
        ListIterator<E> it = arrayListIt.listIterator(i);
        it.next();
        E value = adds.get(adds.size() - i - 1);
        it.set(value);
      }
      for (int i = 0; i < adds.size(); ++i)
      {
        E value = adds.get(adds.size() - i - 1);
        assertEquals(arrayListIt.get(i), value);
      }
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @SuppressWarnings("unchecked")
  public <ArrayTemplate extends AbstractArrayTemplate<E>, E>
  void testArrayBadInput(Class<ArrayTemplate> templateClass,
                         ArrayDataSchema schema,
                         List<E> good,
                         List<Object> badInput,
                         List<Object> badOutput)
  {
    try
    {
      Exception exc = null;
      ArrayTemplate arrayTemplateBad = templateClass.getDeclaredConstructor().newInstance();
      DataList badDataList = new DataList();
      ArrayTemplate badWrappedArrayTemplate = DataTemplateUtil.wrap(badDataList, schema, templateClass);

      List<E> badIn = (List<E>) badInput;

      // add(E element)
      for (E o : badIn)
      {
        try
        {
          exc = null;
          arrayTemplateBad.add(o);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(o == null || exc instanceof ClassCastException);
        assertTrue(o != null || exc instanceof NullPointerException);
      }

      // add(int index, E element)
      for (Object o : badIn)
      {
        try
        {
          exc = null;
          arrayTemplateBad.add(0, (E) o);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(o == null || exc instanceof ClassCastException);
        assertTrue(o != null || exc instanceof NullPointerException);
      }

      // addAll(Collection<E> c)
      try
      {
        exc = null;
        arrayTemplateBad.addAll(badIn);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof ClassCastException);

      // set(int index, E element)
      arrayTemplateBad.addAll(good);
      assertTrue(arrayTemplateBad.size() > 1);
      for (Object o : badIn)
      {
        try
        {
          exc = null;
          arrayTemplateBad.set(0, (E) o);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(o == null || exc instanceof ClassCastException);
        assertTrue(o != null || exc instanceof NullPointerException);
      }

      // listIterator add
      for (Object o : badIn)
      {
        try
        {
          exc = null;
          ListIterator<E> it = arrayTemplateBad.listIterator(0);
          it.add((E) o);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(o == null || exc instanceof ClassCastException);
        assertTrue(o != null || exc instanceof NullPointerException);
      }

      // listIterator set
      for (Object o : badIn)
      {
        try
        {
          exc = null;
          ListIterator<E> it = arrayTemplateBad.listIterator(0);
          it.next();
          it.set((E) o);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(o == null || exc instanceof ClassCastException);
        assertTrue(o != null || exc instanceof NullPointerException);
      }

      badDataList.clear();
      badDataList.addAll(badOutput);
      badWrappedArrayTemplate = DataTemplateUtil.wrap(badDataList, schema, templateClass);

      // Get returns bad
      for (int i = 0; i < badWrappedArrayTemplate.size(); ++i)
      {
        try
        {
          exc = null;
          badWrappedArrayTemplate.get(i);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }

      // Set returns bad
      badDataList.clear();
      badDataList.addAll(badOutput);
      assertEquals(badWrappedArrayTemplate.size(), badOutput.size());
      for (int i = 0; i < badWrappedArrayTemplate.size(); ++i)
      {
        try
        {
          exc = null;
          badWrappedArrayTemplate.set(i, good.get(0));
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }

      // Remove returns bad
      badDataList.clear();
      badDataList.addAll(badOutput);
      assertEquals(badWrappedArrayTemplate.size(), badOutput.size());
      for (int i = 0; i < badWrappedArrayTemplate.size(); ++i)
      {
        try
        {
          exc = null;
          badWrappedArrayTemplate.remove(0);
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }

      // Iterator returns bad
      for (Object o : badOutput)
      {
        badDataList.clear();
        badDataList.add(o);
        try
        {
          exc = null;
          badWrappedArrayTemplate.iterator().next();
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }

      // ListIterator returns bad
      for (Object o : badOutput)
      {
        badDataList.clear();
        badDataList.add(o);
        try
        {
          exc = null;
          badWrappedArrayTemplate.listIterator().next();
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }
      for (Object o : badOutput)
      {
        badDataList.clear();
        badDataList.add(o);
        try
        {
          exc = null;
          badWrappedArrayTemplate.listIterator(badWrappedArrayTemplate.size()).previous();
        }
        catch (Exception e)
        {
          exc = e;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }
    } catch (IllegalAccessException | TemplateOutputCastException | InstantiationException | InvocationTargetException | NoSuchMethodException exc) {
      fail("Unexpected exception", exc);
    }
  }

  @SuppressWarnings("unchecked")
  public <ArrayTemplate extends AbstractArrayTemplate<E>, E extends Number>
  void testNumberArray(Class<ArrayTemplate> templateClass,
                       ArrayDataSchema schema,
                       List<E> castTo,
                       List<? extends Number> castFrom)
  {
    try
    {
      // test insert non-native, converted to element type on set
      ArrayTemplate array1 = templateClass.getDeclaredConstructor().newInstance();
      array1.addAll((List<E>) castFrom);
      for (int i = 0; i < castTo.size(); ++i)
      {
        assertEquals(castTo.get(i), array1.get(i));
        assertEquals(array1.data().get(i), castTo.get(i));
      }

      // test underlying is non-native, convert on get to element type on get.
      DataList dataList2 = new DataList(castFrom);
      ArrayTemplate array2 = DataTemplateUtil.wrap(dataList2, schema, templateClass);
      for (int i = 0; i < castTo.size(); ++i)
      {
        assertSame(dataList2.get(i), castFrom.get(i));
        assertEquals(castTo.get(i), array2.get(i));
      }
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @Test
  public void testBooleanArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"boolean\" }");

    List<Boolean> input = Arrays.asList(true, false); // must be unique
    List<Boolean> adds = Arrays.asList(false, true, true, false);
    List<Object> badInput = asList(1, 2L, 3f, 4.0, "hello", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(1, 2L, 3f, 4.0, "hello", ByteString.empty(), new DataMap(), new DataList());

    testArray(BooleanArray.class, schema, input, adds);
    testArrayBadInput(BooleanArray.class, schema, input, badInput, badOutput);
  }

  @Test
  public void testIntegerArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"int\" }");

    List<Integer> input = Arrays.asList(1, 3, 5, 7, 11); // must be unique
    List<Integer> adds = Arrays.asList(13, 17, 19);
    List<Object> badInput = asList(true, "hello", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, "hello", ByteString.empty(), new DataMap(), new DataList());

    testArray(IntegerArray.class, schema, input, adds);
    testArrayBadInput(IntegerArray.class, schema, input, badInput, badOutput);

    @SuppressWarnings("unchecked")
    List<? extends Number> castFrom = Arrays.asList(1L, 3.0f, 5.0, 7, 11);
    testNumberArray(IntegerArray.class, schema, input, castFrom);
  }

  @Test
  public void testLongArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"long\" }");

    List<Long> input = Arrays.asList(1L, 3L, 5L, 7L, 11L); // must be unique
    List<Long> adds = Arrays.asList(13L, 17L, 19L);
    List<Object> badInput = asList(true, "hello", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, "hello", ByteString.empty(), new DataMap(), new DataList());

    testArray(LongArray.class, schema, input, adds);
    testArrayBadInput(LongArray.class, schema, input, badInput, badOutput);

    @SuppressWarnings("unchecked")
    List<? extends Number> castFrom = Arrays.asList(1, 3.0f, 5.0, 7L, 11L);
    testNumberArray(LongArray.class, schema, input, castFrom);
  }

  @Test
  public void testFloatArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"float\" }");

    List<Float> input = Arrays.asList(1.0f, 3.0f, 5.0f, 7.0f, 11.0f); // must be unique
    List<Float> adds = Arrays.asList(13.0f, 17.0f, 19.0f);
    List<Object> badInput = asList(true, "hello", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, "hello", ByteString.empty(), new DataMap(), new DataList());

    testArray(FloatArray.class, schema, input, adds);
    testArrayBadInput(FloatArray.class, schema, input, badInput, badOutput);

    @SuppressWarnings("unchecked")
    List<? extends Number> castFrom = Arrays.asList(1, 3L, 5.0, 7.0f, 11.0f);
    testNumberArray(FloatArray.class, schema, input, castFrom);
  }

  @Test
  public void testDoubleArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"double\" }");

    List<Double> input = Arrays.asList(1.0, 3.0, 5.0, 7.0, 11.0); // must be unique
    List<Double> adds = Arrays.asList(13.0, 17.0, 19.0);
    List<Object> badInput = asList(true, "hello", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, "hello", ByteString.empty(), new DataMap(), new DataList());

    testArray(DoubleArray.class, schema, input, adds);
    testArrayBadInput(DoubleArray.class, schema, input, badInput, badOutput);

    @SuppressWarnings("unchecked")
    List<? extends Number> castFrom = Arrays.asList(1, 3L, 5.0f, 7.0, 11.0);
    testNumberArray(DoubleArray.class, schema, input, castFrom);
  }

  @Test
  public void testStringArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"string\" }");

    List<String> input = Arrays.asList("apple", "banana", "orange", "pineapple", "graphs"); // must be unique
    List<String> adds = Arrays.asList("foo", "bar", "baz");
    List<Object> badInput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new DataMap(), new DataList());

    testArray(StringArray.class, schema, input, adds);
    testArrayBadInput(StringArray.class, schema, input, badInput, badOutput);
  }

  @Test
  public void testBytesArray()
  {
    ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"bytes\" }");

    List<ByteString> input = Arrays.asList(ByteString.copyAvroString("1", false), ByteString.copyAvroString("3", false), ByteString.copyAvroString("5", false), ByteString.copyAvroString("7", false), ByteString.copyAvroString("11", false));
    List<ByteString> adds = Arrays.asList(ByteString.copyAvroString("13", false), ByteString.copyAvroString("17", false), ByteString.copyAvroString("19", false));
    List<Object> badInput = asList(true, 99, 999L, 88.0f, 888.0, "\u0100", new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, 99, 999L, 88.0f, 888.0, "\u0100", new DataMap(), new DataList());

    testArray(BytesArray.class, schema, input, adds);
    testArrayBadInput(BytesArray.class, schema, input, badInput, badOutput);
  }

  public static enum Fruits
  {
    APPLE, ORANGE, BANANA, GRAPES, PINEAPPLE
  }

  public static class EnumArrayTemplate extends com.linkedin.data.template.DirectArrayTemplate<Fruits>
  {
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\", \"GRAPES\", \"PINEAPPLE\" ] } }");
    public EnumArrayTemplate()
    {
      this(new DataList());
    }
    public EnumArrayTemplate(int capacity)
    {
      this(new DataList(capacity));
    }
    public EnumArrayTemplate(Collection<Fruits> c)
    {
      this(new DataList(c.size()));
      addAll(c);
    }
    public EnumArrayTemplate(DataList list)
    {
      super(list, SCHEMA, Fruits.class, String.class);
    }
    @Override
    public EnumArrayTemplate clone() throws CloneNotSupportedException
    {
      return (EnumArrayTemplate) super.clone();
    }
    @Override
    public EnumArrayTemplate copy() throws CloneNotSupportedException
    {
      return (EnumArrayTemplate) super.copy();
    }
  }

  @Test
  public void testEnumArray()
  {
    List<Fruits> input = Arrays.asList(Fruits.APPLE, Fruits.ORANGE, Fruits.BANANA); // must be unique
    List<Fruits> adds = Arrays.asList(Fruits.GRAPES, Fruits.PINEAPPLE);
    List<Object> badInput = asList(true, 1, 2L, 3.0f, 4.0, "orange", ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, 1, 2L, 3.0f, 4.0, "orange", ByteString.empty(), new DataMap(), new DataList());

    testArray(EnumArrayTemplate.class, TestArrayTemplate.EnumArrayTemplate.SCHEMA, input, adds);
    testArrayBadInput(EnumArrayTemplate.class, TestArrayTemplate.EnumArrayTemplate.SCHEMA, input, badInput, badOutput);
  }

  public static class ArrayOfStringArrayTemplate extends WrappingArrayTemplate<StringArray>
  {
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : { \"type\" : \"array\", \"items\" : \"string\" } }");
    public ArrayOfStringArrayTemplate()
    {
      this(new DataList());
    }
    public ArrayOfStringArrayTemplate(int capacity)
    {
      this(new DataList(capacity));
    }
    public ArrayOfStringArrayTemplate(Collection<StringArray> c)
    {
      this(new DataList(c.size()));
      addAll(c);
    }
    public ArrayOfStringArrayTemplate(DataList list)
    {
      super(list, SCHEMA, StringArray.class);
    }
    @Override
    public ArrayOfStringArrayTemplate clone() throws CloneNotSupportedException
    {
      return (ArrayOfStringArrayTemplate) super.clone();
    }
    @Override
    public ArrayOfStringArrayTemplate copy() throws CloneNotSupportedException
    {
      return (ArrayOfStringArrayTemplate) super.copy();
    }
  }

  @Test
  public void testArrayOfStringArray()
  {
    List<StringArray> input = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      input.add(new StringArray("input" + i));
    }
    List<StringArray> adds = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      adds.add(new StringArray("add" + i));
    }
    List<Object> badInput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new StringMap(), new IntegerArray(), null);
    List<Object> badOutput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new DataMap());

    testArray(ArrayOfStringArrayTemplate.class, ArrayOfStringArrayTemplate.SCHEMA, input, adds);
    testArrayBadInput(ArrayOfStringArrayTemplate.class, ArrayOfStringArrayTemplate.SCHEMA, input, badInput, badOutput);
  }

  public static class FooRecord extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"string\" } ] }");
    public static final RecordDataSchema.Field FIELD_bar = SCHEMA.getField("bar");

    public FooRecord()
    {
      super(new DataMap(), SCHEMA);
    }
    public FooRecord(DataMap map)
    {
      super(map, SCHEMA);
    }

    public String getBar(GetMode mode)
    {
      return obtainDirect(FIELD_bar, String.class, mode);
    }

    public void removeBar()
    {
      remove(FIELD_bar);
    }

    public void setBar(String value)
    {
      putDirect(FIELD_bar, String.class, value);
    }

    @Override
    public FooRecord clone() throws CloneNotSupportedException
    {
      return (FooRecord) super.clone();
    }

    @Override
    public FooRecord copy() throws CloneNotSupportedException
    {
      return (FooRecord) super.copy();
    }
  }

  public static class FooRecordArray extends WrappingArrayTemplate<FooRecord>
  {
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"string\" } ] } }");
    public FooRecordArray()
    {
      this(new DataList());
    }
    public FooRecordArray(int capacity)
    {
      this(new DataList(capacity));
    }
    public FooRecordArray(Collection<FooRecord> c)
    {
      this(new DataList(c.size()));
      addAll(c);
    }
    public FooRecordArray(DataList list)
    {
      super(list, SCHEMA, FooRecord.class);
    }
    @Override
    public FooRecordArray clone() throws CloneNotSupportedException
    {
      return (FooRecordArray) super.clone();
    }
    @Override
    public FooRecordArray copy() throws CloneNotSupportedException
    {
      return (FooRecordArray) super.copy();
    }
  }

  @Test
  public void testFooRecordArray()
  {
    List<FooRecord> input = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      input.add(new FooRecord());
      input.get(i).setBar("input " + i);
    }
    List<FooRecord> adds = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      adds.add(new FooRecord());
      adds.get(i).setBar("add " + i);
    }
    List<Object> badInput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new StringMap(), new StringArray(), null);
    List<Object> badOutput = asList(true, 1, 2L, 3.0f, 4.0, ByteString.empty(), new DataList());

    testArray(FooRecordArray.class, FooRecordArray.SCHEMA, input, adds);
    testArrayBadInput(FooRecordArray.class, FooRecordArray.SCHEMA, input, badInput, badOutput);
  }

  protected static class PrimitiveLegacyArray<T> extends DirectArrayTemplate<T>
  {
    public PrimitiveLegacyArray(DataList list, ArrayDataSchema schema, Class<T> itemsClass)
    {
      super(list, schema, itemsClass);
      assertSame(_dataClass, itemsClass);
    }
  }

  protected static class EnumLegacyArray extends DirectArrayTemplate<Fruits>
  {
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\", \"GRAPES\", \"PINEAPPLE\" ] } }");
    public EnumLegacyArray(DataList list)
    {
      super(list, SCHEMA, Fruits.class);
      assertSame(_dataClass, String.class);
    }
  }

  @Test
  public void testLegacyConstructor()
  {
    Map<String, Class<?>> primitiveStringToClassMap = asMap(
      "int", Integer.class,
      "long", Long.class,
      "float", Float.class,
      "double", Double.class,
      "boolean", Boolean.class,
      "string", String.class);
    for (Map.Entry<String, Class<?>> e : primitiveStringToClassMap.entrySet())
    {
      ArrayDataSchema schema = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"" + e.getKey() + "\" }");
      @SuppressWarnings("unchecked")
      PrimitiveLegacyArray<?> array = new PrimitiveLegacyArray<>(new DataList(), schema, (Class<?>) e.getValue());
    }
    EnumLegacyArray enumArray = new EnumLegacyArray(new DataList());
  }

  @SuppressWarnings("serial")
  public static class InternalList<E> extends ArrayList<E>
  {
    InternalList(List<E> l)
    {
      super(l);
    }
    public void removeRange(int from, int to)
    {
      super.removeRange(from, to);
    }
  }
}
