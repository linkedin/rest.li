package com.linkedin.restli.example.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.example.AlbumEntry;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.InjectResourceFactory;

public class TestAlbumEntryResource
{
  private PhotoResource                       _photoRes;
  private AlbumResource                       _albumRes;
  private AlbumEntryResource                  _entryRes;

  private static Map<String, ResourceModel> buildResourceModels(Class<?>... rootResourceClasses)
  {
    final Map<String, ResourceModel> map = new HashMap<String, ResourceModel>();
    for (Class<?> rootResourceClass : rootResourceClasses)
    {
      final ResourceModel model = RestLiAnnotationReader.processResource(rootResourceClass);
      map.put("/" + model.getName(), model);
    }

    return map;
  }

  // function annotated with @BeforeMethod will run once before any test starts
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
    final Map<String, ResourceModel> pathRootResourceMap =
        buildResourceModels(PhotoResource.class, AlbumResource.class, AlbumEntryResource.class);
    factory.setRootResources(pathRootResourceMap);

    _photoRes = factory.create(PhotoResource.class);
    Assert.assertNotNull(_photoRes);
    Assert.assertNotNull(_photoRes.getDb());

    _albumRes = factory.create(AlbumResource.class);
    Assert.assertNotNull(_albumRes);
    Assert.assertNotNull(_albumRes.getDb());

    _entryRes = factory.create(AlbumEntryResource.class);
    Assert.assertNotNull(_entryRes);

    makeData();
  }

  // some data we'll reference
  private AlbumEntry[]  _entries;
  private CompoundKey[] _keys;

  private void makeData()
  {
    _entries =
        new AlbumEntry[] { new AlbumEntry().setAddTime(1), new AlbumEntry().setAddTime(2),
            new AlbumEntry().setAddTime(3), new AlbumEntry().setAddTime(4),
            new AlbumEntry().setAddTime(5) };
    _keys =
        new CompoundKey[] { new CompoundKey().append("photoId", 1L).append("albumId", 1L),
            new CompoundKey().append("photoId", 2L).append("albumId", 1L),
            new CompoundKey().append("photoId", 3L).append("albumId", 1L),
            new CompoundKey().append("photoId", 1L).append("albumId", 2L),
            new CompoundKey().append("photoId", 4L).append("albumId", 2L) };

    // make some album entries

    for (int i = 0; i < _entries.length; i++)
    {
      final UpdateResponse uResp = _entryRes.update(_keys[i], _entries[i]);
      Assert.assertEquals(uResp.getStatus(), HttpStatus.S_204_NO_CONTENT);
    }
  }

  @Test
  public void testUpdateGet()
  {
    // validate data is set to correct value
    for (int i = 0; i < _entries.length; i++)
    {
      Assert.assertEquals(_entryRes.get(_keys[i]), _entries[i]);
    }
  }

  @Test(expectedExceptions = RestLiServiceException.class)
  public void testBadUpdatePhotoId()
  {
    // photo 100 doesn't exist
    CompoundKey key = new CompoundKey().append("photoId", 100L).append("albumId", 1L);
    AlbumEntry entry = new AlbumEntry().setAddTime(4);
    _entryRes.update(key, entry);
  }

  @Test(expectedExceptions = RestLiServiceException.class)
  public void testBadUpdateAlbumId()
  {
    // album 100 doesn't exist
    CompoundKey key = new CompoundKey().append("photoId", 1L).append("albumId", 100L);
    AlbumEntry entry = new AlbumEntry().setAddTime(4);
    _entryRes.update(key, entry);
  }

  @Test(expectedExceptions = RestLiServiceException.class)
  public void testBadUpdateIdsInEntry()
  {
    // shouldn't be able to put IDs in update entry
    CompoundKey key = new CompoundKey().append("photoId", 1L).append("albumId", 1L);
    AlbumEntry entry = new AlbumEntry().setAddTime(4).setPhotoId(1);
    _entryRes.update(key, entry);
  }

  @Test
  public void testBatchGet()
  {
    // get keys 1-3
    Set<CompoundKey> batchIds = new HashSet<CompoundKey>();
    for (int i = 1; i <= 3; i++)
    {
      batchIds.add(_keys[i]);
    }
    Map<CompoundKey, AlbumEntry> batchEntries = _entryRes.batchGet(batchIds);

    Assert.assertEquals(batchEntries.size(), 3);
    for (int i = 1; i <= 3; i++)
    {
      Assert.assertEquals(batchEntries.get(_keys[i]), _entries[i]);
    }
  }

  @Test
  public void testSearch()
  {
    // we previously put the first 3 entries in album 1
    Set<AlbumEntry> result = new HashSet<AlbumEntry>(_entryRes.search(Long.valueOf(1), null));
    Set<AlbumEntry> expected = new HashSet<AlbumEntry>();
    for (int i = 0; i < 3; i++)
    {
      expected.add(_entries[i]);
    }
    Assert.assertEquals(result, expected);
  }

  @Test
  public void testResourcePurge()
  {
    // we put 2 photos in album 2; delete them
    Assert.assertEquals(_entryRes.purge(Long.valueOf(2), null), 2);
  }
}