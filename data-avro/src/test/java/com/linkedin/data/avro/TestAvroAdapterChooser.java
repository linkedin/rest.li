package com.linkedin.data.avro;

public class TestAvroAdapterChooser implements AvroAdapterChooser
{
  static class MyAvroAdapter extends TestAvroAdapter
  {
  }

  private final MyAvroAdapter _avroAdapter = new MyAvroAdapter();

  @Override
  public AvroAdapter getAvroAdapter()
  {
    return _avroAdapter;
  }
}
