package com.linkedin.data.template;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.util.ArgumentUtil;
import java.util.Arrays;
import java.util.Collection;

/**
 * {@link DataTemplate} for a ByteString array.
 */
public class ByteStringArray extends DirectArrayTemplate<ByteString> {
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"bytestring\" }");

  public ByteStringArray()
  {
    this(new DataList());
  }

  public ByteStringArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public ByteStringArray(Collection<ByteString> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public ByteStringArray(DataList list)
  {
    super(list, SCHEMA, ByteString.class, ByteString.class);
  }

  public ByteStringArray(ByteString first, ByteString... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public ByteStringArray clone() throws CloneNotSupportedException
  {
    return (ByteStringArray) super.clone();
  }

  @Override
  public ByteStringArray copy() throws CloneNotSupportedException
  {
    return (ByteStringArray) super.copy();
  }

  @Override
  protected Object coerceInput(ByteString object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return object;
  }

  @Override
  protected ByteString coerceOutput(Object object) throws TemplateOutputCastException
  {
    assert(object != null);
    return DataTemplateUtil.coerceBytesOutput(object);
  }
}
