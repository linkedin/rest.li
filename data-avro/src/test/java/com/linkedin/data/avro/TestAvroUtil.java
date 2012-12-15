package com.linkedin.data.avro;

public class TestAvroUtil
{
  public static String namespaceProcessor(String text)
  {
    if (text.contains("##NS"))
    {
      final AvroAdapter avroAdapter = AvroAdapterFinder.getAvroAdapter();

      if (avroAdapter.jsonUnionMemberHasFullName())
        text = text.replaceAll("##NS\\(([^\\)]+)\\)", "$1");
      else
        text = text.replaceAll("##NS\\([^\\)]+\\)", "");
    }
    return text;
  }
}
