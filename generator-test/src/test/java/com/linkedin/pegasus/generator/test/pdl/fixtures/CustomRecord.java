package com.linkedin.pegasus.generator.test.pdl.fixtures;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import java.util.Collections;
import java.util.List;


public class CustomRecord implements DataTemplate<DataMap>
{
  private final String _title;
  private final String _body;

  public CustomRecord(String title, String body)
  {
    _title = title;
    _body = body;
  }

  public CustomRecord(DataMap dataMap)
  {
    _title = dataMap.getString("title");
    _body = dataMap.getString("body");
  }

  public String getTitle()
  {
    return _title;
  }

  public String getBody()
  {
    return _body;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    CustomRecord that = (CustomRecord) o;

    if (!_title.equals(that._title))
    {
      return false;
    }
    return _body.equals(that._body);
  }

  @Override
  public int hashCode()
  {
    int result = _title.hashCode();
    result = 31 * result + _body.hashCode();
    return result;
  }

  public static CustomRecord.Fields fields()
  {
    return new Fields();
  }

  public static class Fields extends PathSpec
  {
    public Fields(List<String> path, String name)
    {
      super(path, name);
    }

    public Fields()
    {
      super();
    }

    public PathSpec title()
    {
      return new PathSpec(getPathComponents(), "title");
    }

    public PathSpec body()
    {
      return new PathSpec(getPathComponents(), "body");
    }
  }

  @Override
  public DataSchema schema()
  {
    return null;
  }

  @Override
  public DataMap data()
  {
    DataMap dataMap = new DataMap();
    dataMap.put("title", _title);
    dataMap.put("body", _body);
    return dataMap;
  }

  @Override
  public DataTemplate<DataMap> clone()
      throws CloneNotSupportedException
  {
    return null;
  }

  @Override
  public DataTemplate<DataMap> copy()
      throws CloneNotSupportedException
  {
    return null;
  }
}
