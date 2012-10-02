package com.linkedin.restli.example.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.example.Album;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

// This code is very similar to PhotoResource. To keep this example simple, we simply duplicate it
// instead of using a generic subclass.
@RestLiCollection(name = "albums", namespace = "com.linkedin.restli.example.photos")
public class AlbumResource extends CollectionResourceTemplate<Long, Album>
{
  Logger _log = LoggerFactory.getLogger(AlbumResource.class);

  public AlbumDatabase getDb()
  {
    return _db;
  }

  // basic overridable functions for resource template
  // corresponding builder class are generated to src/mainGeneratedRest/java/<namespace>
  @Override
  public CreateResponse create(Album entity)
  {
    final Long newId = _db.getCurrentId();

    if (entity.hasId() || entity.hasUrn())
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Album ID is not acceptable in request");
    }

    // overwrite ID and URN in entity
    entity.setId(newId);
    entity.setUrn(String.valueOf(newId));
    _db.getData().put(newId, entity);
    return new CreateResponse(newId);
  }

  // return stored photo
  // if the key does not exist, return null, and rest.li will respond HTTP 404 to client
  @Override
  public Album get(Long key)
  {
    _log.info("Getting album # " + key);

    return _db.getData().get(key);
  }

  // update an existing photo with given entity
  @Override
  public UpdateResponse update(Long key, Album entity)
  {
    final Album currPhoto = _db.getData().get(key);
    if (currPhoto == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    // disallow changing entity ID and URN
    if (entity.hasId() || entity.hasUrn())
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Album ID is not acceptable in request");
    }

    // make sure the ID in the entity is consistent with the key in the database
    entity.setId(key);
    entity.setUrn(String.valueOf(key));
    _db.getData().put(key, entity);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  // delete an existing album
  @Override
  public UpdateResponse delete(Long key)
  {
    final boolean isRemoved = (_db.getData().remove(key) != null);

    // Remove all entries in this album to maintain referential integrity
    AlbumEntryResource.purge(_entryDb, key, null);

    return new UpdateResponse(isRemoved ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  // custom action defined on collection level without any parameter
  // call with "http://<hostname>:<port>/photos?action=purge"
  // return JSON object of the action result
  // if called on wrong resource level, HTTP 400 is responded
  @Action(name = "purge", resourceLevel = ResourceLevel.COLLECTION)
  public int purge()
  {
    final int numPurged = _db.getData().size();
    _db.getData().clear();
    AlbumEntryResource.purge(_entryDb, null, null);
    return numPurged;
  }

  public final static String URN_ENTITY_TYPE = "album";

  // use dependency injection instead of hard-coding database instance
  @Inject
  @Named("albumDb")
  private AlbumDatabase      _db;

  // need this to cascade deletes
  @Inject
  @Named("albumEntryDb")
  private AlbumEntryDatabase _entryDb;
}
