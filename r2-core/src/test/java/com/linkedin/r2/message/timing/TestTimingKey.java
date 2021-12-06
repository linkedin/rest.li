package com.linkedin.r2.message.timing;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link TimingKey}
 */
public class TestTimingKey
{
  @Test
  public void testGetUniqueName()
  {
    final Set<String> names = new HashSet<>();

    for (int i = 0; i < 10000; i++) {
      final String uniqueName = TimingKey.getUniqueName("baseName");

      Assert.assertTrue(uniqueName.contains("baseName"));
      Assert.assertFalse(names.contains(uniqueName));

      names.add(uniqueName);
    }
  }

}
