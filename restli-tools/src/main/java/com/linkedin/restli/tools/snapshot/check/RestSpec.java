package com.linkedin.restli.tools.snapshot.check;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.NamedDataSchema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


/**
 * @author Keren Jin
 */
public class RestSpec extends AbstractSnapshot
{
  public RestSpec(InputStream inputStream) throws IOException
  {
    DataMap data = _dataCodec.readMap(inputStream);
    _models = new HashMap<String, NamedDataSchema>();
    _resourceSchema = parseSchema(data);
  }
}
