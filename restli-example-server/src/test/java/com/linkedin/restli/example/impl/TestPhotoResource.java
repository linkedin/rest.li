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

package com.linkedin.restli.example.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.example.EXIF;
import com.linkedin.restli.example.LatLong;
import com.linkedin.restli.example.Photo;
import com.linkedin.restli.example.PhotoFormats;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.InjectResourceFactory;

public class TestPhotoResource
{
  private PhotoResource _res;

  private static Map<String, ResourceModel> buildResourceModels(Class<?>... rootResourceClasses)
  {
    final Map<String, ResourceModel> map = new HashMap<>();
    for (Class<?> rootResourceClass : rootResourceClasses)
    {
      final ResourceModel model = RestLiAnnotationReader.processResource(rootResourceClass);
      map.put("/" + model.getName(), model);
    }

    return map;
  }

  private Long createPhoto()
  {
    // create a new photo and confirm photo ID
    return createPhoto("Test Photo");
  }

  private Long createPhoto(String title)
  {
      return createPhoto(title, PhotoFormats.PNG);
  }

  private Long createPhoto(String title, PhotoFormats format)
  {
      final LatLong l = new LatLong().setLatitude(7.0f).setLongitude(27.0f);
      final EXIF e = new EXIF().setIsFlash(true).setLocation(l);
      final Photo p = new Photo().setTitle(title).setFormat(format).setExif(e);
      final CreateResponse cResp = _res.create(p);
      Assert.assertTrue(cResp.hasId());
      return (Long) cResp.getId();
  }

  // function annotated with @BeforeMethod will run once before each test starts
  @BeforeMethod
  public void initResource()
  {
    // the photo resource requires dependency injection to work
    // we use InjectResourceFactory from pegasus to manually inject the dependency
    SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    final PhotoDatabase photoDb = new PhotoDatabaseImpl(10);
    beanProvider.add("photoDb", photoDb);
    beanProvider.add("albumDb", new AlbumDatabaseImpl(10));
    beanProvider.add("albumEntryDb", new AlbumEntryDatabaseImpl(photoDb, 3));

    final InjectResourceFactory factory = new InjectResourceFactory(beanProvider);

    final Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(PhotoResource.class);
    factory.setRootResources(pathRootResourceMap);

    _res = factory.create(PhotoResource.class);
    Assert.assertNotNull(_res);
    Assert.assertNotNull(_res.getDb());
  }

  @Test
  public void testResourceGet()
  {
    // because the test function will take arbitrary order
    // always create a photo and operate on that photo for a test function
    final Long id = createPhoto();

    // validate all data are correct
    final Photo p = _res.get(id);
    Assert.assertNotNull(p);
    Assert.assertEquals(p.getId(), id);
    Assert.assertTrue(p.hasExif());
    final EXIF e = p.getExif();
    Assert.assertTrue(e.hasLocation());
    final LatLong l = e.getLocation();
    Assert.assertEquals(l.getLatitude(), 7.0f);
    Assert.assertEquals(l.getLongitude(), 27.0f);
  }

  @Test
  public void testBatchGet() {
    final String[] titles = {"1","2","3"};
    final long[] ids = new long[titles.length];
    for (int i = 0; i < titles.length; i++)
      ids[i] = createPhoto(titles[i]);

    // validate all data are correct
    Set<Long> batchIds = new HashSet<>();
    batchIds.add(ids[1]);
    batchIds.add(ids[2]);
    Map<Long, Photo> batchPhotos = _res.batchGet(batchIds);

    Assert.assertEquals(batchPhotos.size(), 2);

    for (int i = 1; i < titles.length; i++) // go through {1,2}
    {
      final Photo p = batchPhotos.get(ids[i]);
      Assert.assertNotNull(p);
      Assert.assertEquals(p.getTitle(), titles[i]);
      Assert.assertEquals(p.getId().longValue(), ids[i]);
      Assert.assertTrue(p.hasExif());
      final EXIF e = p.getExif();
      Assert.assertTrue(e.hasLocation());
      final LatLong l = e.getLocation();
      Assert.assertEquals(l.getLatitude(), 7.0f);
      Assert.assertEquals(l.getLongitude(), 27.0f);
    }
  }

  @Test
  public void testResourceUpdate()
  {
    final Long id = createPhoto();

    final LatLong l1 = new LatLong().setLongitude(-27.0f);
    final EXIF e1 = new EXIF().setLocation(l1);
    final Photo p1 = new Photo().setExif(e1);
    final UpdateResponse uResp = _res.update(id, p1);
    Assert.assertEquals(uResp.getStatus(), HttpStatus.S_204_NO_CONTENT);

    // validate data is changed to correct value
    final Photo p2 = _res.get(id);
    Assert.assertNotNull(p2.hasExif());
    final EXIF e2 = p2.getExif();
    Assert.assertNotNull(e2.hasLocation());
    final LatLong l2 = e2.getLocation();
    Assert.assertEquals(l2.getLongitude(), -27.0f);
  }

  @Test
  public void testResourceDelete()
  {
    final Long id = createPhoto();

    // validate response status code
    final UpdateResponse uResp = _res.delete(id);
    Assert.assertEquals(uResp.getStatus(), HttpStatus.S_204_NO_CONTENT);
  }


  @Test
  public void testResourcePartialUpdate()
  {
    final Long id = createPhoto("PartialTestPhoto");
    final String newTitle = "The New Title";
    final Photo p = new Photo().setTitle(newTitle);
    final PatchRequest<Photo> patch = PatchGenerator.diffEmpty(p);
    final UpdateResponse uResp = _res.update(id, patch);

    final Photo updatedPhoto = _res.get(id);
    Assert.assertEquals(updatedPhoto.getTitle(), newTitle);
    Assert.assertEquals(uResp.getStatus(), HttpStatus.S_202_ACCEPTED);
  }


  @Test
  public void testResourceFind()
  {
      _res.purge();
      createPhoto("InEdible");
      createPhoto("InEdible");
      createPhoto("InEdible", PhotoFormats.BMP);

      final PagingContext pc = new PagingContext(0, 4);
      final PagingContext pc2 = new PagingContext(0, 2);

      final List<Photo> foundByTitle = _res.find(pc, "InEdible", null);
      Assert.assertEquals(foundByTitle.size(), 3);

      final List<Photo> foundByFormat = _res.find(pc, null, PhotoFormats.BMP);
      Assert.assertEquals(foundByFormat.size(), 1);

      final List<Photo> foundByTitleAndFormat = _res.find(pc, "InEdible", PhotoFormats.PNG);
      Assert.assertEquals(foundByTitleAndFormat.size(), 2);


      final List<Photo> foundTwo = _res.find(pc2, null, null);
      Assert.assertEquals(foundTwo.size(), 2);

      // testResourcePurge() assumes there is at least one photo
      createPhoto();
  }

  @Test
  public void testResourcePurge()
  {
    // photo database is initialized to have 1 photo
    // at any time of the test, that initial photo should present for purge
    Assert.assertTrue(_res.purge() > 0);
  }
}
