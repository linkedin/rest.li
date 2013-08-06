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

package com.linkedin.restli.server.twitter;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;


/**
 * Empty/marker RecordTemplates for Twitter domain
 *
 * @author dellamag
 */
public class TwitterTestDataModels
{
  public static class Status extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("Status", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public Status()
    {
      super(new DataMap(), SCHEMA);
    }

    public Status(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class User extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("User", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public User(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class Followed extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("Followed", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public Followed(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class Location extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("Location", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public Location(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class Trending extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("Trend", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public Trending(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class DiscoveredItem extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("DiscoveredItem", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public DiscoveredItem(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class DiscoveredItemKey extends RecordTemplate
  {
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"record\",\"name\":\"DiscoveredItem\",\"namespace\":\"com.example.fortune\",\"fields\":[{\"name\":\"userId\",\"type\":\"long\"},{\"name\":\"type\",\"type\":\"int\"},{\"name\":\"itemId\",\"type\":\"long\"}]}"));
    private final static RecordDataSchema.Field FIELD_UserId = SCHEMA.getField("userId");
    private final static RecordDataSchema.Field FIELD_Type = SCHEMA.getField("type");
    private final static RecordDataSchema.Field FIELD_ItemId = SCHEMA.getField("itemId");

    public DiscoveredItemKey()
    {
      this(new DataMap());
    }

    public DiscoveredItemKey(DataMap map)
    {
      super(map, SCHEMA);
    }

    public Long getUserId()
    {
      return obtainDirect(FIELD_UserId, Long.class, GetMode.STRICT);
    }

    public Integer getType()
    {
      return obtainDirect(FIELD_Type, Integer.class, GetMode.STRICT);
    }

    public Long getItemId()
    {
      return obtainDirect(FIELD_ItemId, Long.class, GetMode.STRICT);
    }

    public DiscoveredItemKey setUserId(long value)
    {
      putDirect(FIELD_UserId, Long.class, Long.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public DiscoveredItemKey setType(int value)
    {
      putDirect(FIELD_Type, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public DiscoveredItemKey setItemId(long value)
    {
      putDirect(FIELD_ItemId, Long.class, Long.class, value, SetMode.DISALLOW_NULL);
      return this;
    }
  }

  public static class DiscoveredItemKeyParams extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("DiscoveredItemKeyParams", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public DiscoveredItemKeyParams()
    {
      this(new DataMap());
    }

    public DiscoveredItemKeyParams(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public enum StatusType
  {
    STATUS,
    REPLY,
    RETWEET;

    private static final EnumDataSchema SCHEMA = new EnumDataSchema(new Name("StatusType", new StringBuilder(10)));
  }
}