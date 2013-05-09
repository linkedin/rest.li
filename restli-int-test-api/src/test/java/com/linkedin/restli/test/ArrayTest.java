package com.linkedin.restli.test;


import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneFacet;
import com.linkedin.restli.examples.greetings.client.ArrayTestBuilders;
import com.linkedin.restli.examples.greetings.client.ArrayTestFindByTestBuilder;
import org.testng.annotations.Test;

import java.util.Arrays;


/**
 * @author Keren Jin
 */
public class ArrayTest
{
  @Test
  public void test()
  {
    final ArrayTestFindByTestBuilder builders = new ArrayTestBuilders().findByTest();
    builders.primitiveParam(Arrays.asList(1, 2, 3));
    builders.enumParam(Arrays.asList(Tone.FRIENDLY, Tone.SINCERE));
    builders.recordParam(Arrays.asList(new Message(), new Message()));
    builders.existingParam(Arrays.asList(new ToneFacet(), new ToneFacet()));
  }
}
