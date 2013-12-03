package com.linkedin.restli.internal.server.util;

import com.linkedin.data.DataMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Zhenkai Zhu
 */
public class TestDataMapUtils
{
  private static char [] UNMATCHED_SURROGATE_PAIR = {0xd83c, 0xdf70, 0x20, 0xd83c, 0x2e, 0x22, 0x2c, 0x22};
  private static String NORMAL_STRING = "\ud800\udc35best by CT12/14\n\t\n!#$%\nend.";

  @Test
  public void testPermissive()
  {
    DataMap map = new DataMap();
    map.put("initial", new String(UNMATCHED_SURROGATE_PAIR));
    DataMapUtils.mapToBytes(map, true);
  }

  @Test
  public void regressionTest() throws IOException
  {
    DataMap map = new DataMap();
    map.put("test", new String(NORMAL_STRING));

    byte[] strictResult = DataMapUtils.mapToBytes(map);
    byte[] permissiveResult = DataMapUtils.mapToBytes(map, true);

    Assert.assertTrue(Arrays.equals(strictResult, permissiveResult));
  }
}
