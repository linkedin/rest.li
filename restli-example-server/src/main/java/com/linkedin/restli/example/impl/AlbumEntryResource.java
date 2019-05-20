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

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.example.AlbumEntry;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.ParamError;
import com.linkedin.restli.server.annotations.ParamErrors;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.annotations.SuccessResponse;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import static com.linkedin.restli.example.impl.AlbumServiceError.Codes.*;


/**
 * Many-many association between photos and albums.
 *
 * <pre>
 *   new AlbumEntryBuilders().findBySearch()
 *     .albumIdParam(5)
 *     .photoIdParam(100)
 *     .build();
 * </pre>
 *
 * @author jnwang
 */
@RestLiAssociation(
  name = "albumEntry",
  namespace = "com.linkedin.restli.example.photos",
  assocKeys = {
    // The type of the association key should usually be the same as the type of the
    // collection key which is being referenced.For example, if albumId was declared as an
    // Integer in the collection, we would use the following:
    // assocKeys = {
    //   @Key(name = "photoId", type = Long.class),
    //   @Key(name = "albumId", type = Integer.class)
    // }
    @Key(name = "photoId", type = Long.class),
    @Key(name = "albumId", type = Long.class)
  }
)
@ServiceErrorDef(AlbumServiceError.class)
@ServiceErrors(BAD_REQUEST)
public class AlbumEntryResource extends AssociationResourceTemplate<AlbumEntry>
{
  /**
   * Retrieve the photo's album entry
   */
  @Override
  @SuccessResponse(statuses = { 200 })
  public AlbumEntry get(CompoundKey key)
  {
    return _db.getData().get(key);
  }

  @Override
  public Map<CompoundKey, AlbumEntry> batchGet(Set<CompoundKey> ids)
  {
    Map<CompoundKey, AlbumEntry> result = new HashMap<CompoundKey, AlbumEntry>();
    for (CompoundKey key : ids)
      result.put(key, get(key));
    return result;
  }

  /**
   * Add the specified photo to the specified album.
   * If a matching pair of IDs already exists, this changes the add date.
   */
  @Override
  @SuccessResponse(statuses = { 204 })
  public UpdateResponse update(CompoundKey key, AlbumEntry entity)
  {
    long photoId = (Long) key.getPart("photoId");
    long albumId = (Long) key.getPart("albumId");

    // make sure photo and album exist
    if (!_photoDb.getData().containsKey(photoId))
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Nonexistent photo ID: " + photoId);
    if (!_albumDb.getData().containsKey(albumId))
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Nonexistent album ID: " + albumId);

    // disallow changing entity ID
    if (entity.hasAlbumId() || entity.hasPhotoId())
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Photo/album ID are not acceptable in request");

    // make sure the ID in the entity is consistent with the key in the database
    entity.setPhotoId(photoId);
    entity.setAlbumId(albumId);
    _db.getData().put(key, entity);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * Remove the specified photo from the specified album
   */
  @Override
  @SuccessResponse(statuses = { 204 })
  public UpdateResponse delete(CompoundKey key)
  {
    final boolean isRemoved = (_db.getData().remove(key) != null);
    return new UpdateResponse(isRemoved ? HttpStatus.S_204_NO_CONTENT
        : HttpStatus.S_404_NOT_FOUND);
  }

  /**
   * Delete all entries in the db with matching album/photo IDs. <code>null</code> is
   * treated as a wildcard.
   */
  public static int purge(AlbumEntryDatabase db, Long albumId, Long photoId)
  {
    // purge 1 entry
    if (albumId != null && photoId != null)
    {
      CompoundKey key = new CompoundKey().append("photoId", photoId).append("albumId", albumId);
      final boolean isRemoved = (db.getData().remove(key) != null);
      return isRemoved ? 1 : 0;
    }

    // purge all
    if (albumId == null && photoId == null)
    {
      final int numPurged = db.getData().size();
      db.getData().clear();
      return numPurged;
    }

    // purge all matching one of key id, photo id
    Iterator<CompoundKey> it = db.getData().keySet().iterator();
    String partName;
    long compareId;
    if (albumId != null)
    {
      partName = "albumId";
      compareId = albumId;
    }
    else if (photoId != null)
    {
      partName = "photoId";
      compareId = photoId;
    }
    else
      throw new AssertionError();

    int numPurged = 0;
    while (it.hasNext())
    {
      CompoundKey key = it.next();
      if (key.getPart(partName).equals(compareId))
      {
        it.remove();
        numPurged++;
      }
    }
    return numPurged;
  }

  // custom action defined on collection level without any parameter
  // call with "http://<hostname>:<port>/photos?action=purge" and post a JSON file with
  // the IDs
  // return JSON object of the action result
  // if called on wrong resource level, HTTP 400 is responded
  /**
   * Delete all entries in the db with matching album/photo IDs. If either albumId or photoId
   * params are not supplied they are treated as a wildcard.
   *
   */
  @Action(name = "purge", resourceLevel = ResourceLevel.COLLECTION)
  public int purge(@Optional @ActionParam("albumId") Long albumId,
                   @Optional @ActionParam("photoId") Long photoId)
  {
    return purge(_db, albumId, photoId);
  }

  /**
   * Find all entries matching the given album and photo IDs. <code>null</code> is treated
   * as a wildcard.
   *
   * @param albumId provides the id to match for albums to match, if not provided, it is treated as a wildcard
   * @param photoId provides the id to match for photos to match, if not provided, it is treated as a wildcard
   * @return a list of {@link AlbumEntry} matching the  given parameters
   */
  @Finder("search")
  @ServiceErrors(INVALID_PERMISSIONS)
  @ParamError(code = INVALID_ALBUM_ID, parameterNames = { "albumId" })
  public List<AlbumEntry> search(@Optional @QueryParam("albumId") Long albumId,
                                 @Optional @QueryParam("photoId") Long photoId)
  {
    List<AlbumEntry> result = new ArrayList<AlbumEntry>();
    for (Map.Entry<CompoundKey, AlbumEntry> entry : _db.getData().entrySet())
    {
      CompoundKey key = entry.getKey();
      // if the id is null, don't do any filtering by that id
      // (treat all values as a match)
      if (albumId != null && !key.getPart("albumId").equals(albumId))
        continue;
      if (photoId != null && !key.getPart("photoId").equals(photoId))
        continue;
      result.add(entry.getValue());
    }
    return result;
  }

  @Inject
  @Named("albumEntryDb")
  private AlbumEntryDatabase _db;

  @Inject
  @Named("photoDb")
  private PhotoDatabase      _photoDb; // will be same as the PhotoResource db

  @Inject
  @Named("albumDb")
  private AlbumDatabase      _albumDb; // will be same as the AlbumResource db
}
