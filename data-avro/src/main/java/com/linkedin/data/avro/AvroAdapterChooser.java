package com.linkedin.data.avro;

/**
 * Returns an {@link AvroAdapter}.
 *
 * This class provides a way to override how an {@link AvroAdapter} is selected.
 * @see AvroAdapterFinder
 */
interface AvroAdapterChooser
{
  AvroAdapter getAvroAdapter();
}
