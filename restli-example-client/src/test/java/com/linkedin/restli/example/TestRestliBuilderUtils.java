/**
 * $Id: $
 */

package com.linkedin.restli.example;

import com.linkedin.restli.client.util.RestliBuilderUtils;
import com.linkedin.restli.example.photos.PhotosCreateRequestBuilder;
import com.linkedin.restli.example.photos.PhotosRequestBuilders;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author David Hoa
 * @version $Revision: $
 */

public class TestRestliBuilderUtils
{
  @Test
  public void testSubBuilderGet()
  {
    try
    {
      String uri = RestliBuilderUtils.getPrimaryResourceName(PhotosCreateRequestBuilder.class);
      Assert.fail("Shouldn't be able to get the primary resource name from a non-master builder");
    }
    catch (RuntimeException e)
    {
      // expected
    }
  }

  @Test
  public void testPhotoBuilder()
  {
    String serviceName = RestliBuilderUtils.getPrimaryResourceName(PhotosRequestBuilders.class);
    Assert.assertEquals(serviceName, "photos");
  }
}
