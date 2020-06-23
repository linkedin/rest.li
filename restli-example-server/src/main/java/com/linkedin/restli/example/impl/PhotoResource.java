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


import com.linkedin.restli.example.EXIF;
import com.linkedin.restli.example.PhotoCriteria;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.NoMetadata;
import com.linkedin.restli.server.annotations.BatchFinder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.example.Photo;
import com.linkedin.restli.example.PhotoFormats;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;


/**
 * @author kjin
 */
// declare the class to handle collection resources
// collection name "photos", can be any name appropriate
// namespace is used in the generated IDL file, which will be used in the client
// namespace must matches the namespace of the schema
@RestLiCollection(name = "photos", namespace = "com.linkedin.restli.example.photos")
// first template type as the type of the key of the collection resource
// second as the type of the record template itself
public class PhotoResource extends CollectionResourceTemplate<Long, Photo>
{
  public PhotoDatabase getDb()
  {
    return _db;
  }

  // basic overridable functions for resource template
  // corresponding builder class are generated to src/mainGeneratedRest/java/<namespace>
  @Override
  public CreateResponse create(Photo entity)
  {
    final Long newId = _db.getCurrentId();
    //ID and URN are required fields, so use a dummy value to denote "empty" fields
    if ((entity.hasId() && entity.getId() != -1) || (entity.hasUrn() && !entity.getUrn().equals("")))
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Photo ID is not acceptable in request");
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
  public Photo get(Long key)
  {
    return _db.getData().get(key);
  }

  @Override
  public BatchResult<Long, Photo> batchGet(Set<Long> ids)
  {
    Map<Long, Photo> result = new HashMap<Long, Photo>();
    Map<Long, RestLiServiceException> errors =
        new HashMap<Long, RestLiServiceException>();

    for (Long key : ids)
    {
      if (get(key) != null)
      {
        result.put(key, get(key));
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND,
                                                   "No photo with id=" + key
                                                       + " has been found."));
      }
    }
    return new BatchResult<Long, Photo>(result, errors);
  }

  // update an existing photo with given entity
  @Override
  public UpdateResponse update(Long key, Photo entity)
  {
    final Photo currPhoto = _db.getData().get(key);
    if (currPhoto == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }
    //Disallow changing entity ID and URN
    //ID and URN are required fields, so use a dummy value to denote "empty" fields
    if ((entity.hasId() && entity.getId() != -1) || (entity.hasUrn() && !entity.getUrn().equals("")))
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                       "Photo ID is not acceptable in request");
    }

    // make sure the ID in the entity is consistent with the key in the database
    entity.setId(key);
    entity.setUrn(String.valueOf(key));
    _db.getData().put(key, entity);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  // delete an existing photo
  @Override
  public UpdateResponse delete(Long key)
  {
    final boolean isRemoved = (_db.getData().remove(key) != null);

    // Remove this photo from all albums to maintain referential integrity.
    AlbumEntryResource.purge(_entryDb, null, key);

    return new UpdateResponse(isRemoved ? HttpStatus.S_204_NO_CONTENT
        : HttpStatus.S_404_NOT_FOUND);
  }

  // allow partial update to an existing photo
  @Override
  public UpdateResponse update(Long key, PatchRequest<Photo> patchRequest)
  {
    final Photo p = _db.getData().get(key);
    if (p == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    try
    {
      PatchApplier.applyPatch(p, patchRequest);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }
    // photo's id and URN should not be changed
    p.setId(key);
    p.setUrn(String.valueOf(key));
    _db.getData().put(key, p);

    return new UpdateResponse(HttpStatus.S_202_ACCEPTED);
  }

  // find photos by title and/or format
  // if both title and format are empty, any photo meets the search criteria
  @Finder("titleAndOrFormat")
  public List<Photo> find(@PagingContextParam PagingContext pagingContext,
                          @QueryParam("title") @Optional String title,
                          @QueryParam("format") @Optional PhotoFormats format)
  {
    final List<Photo> photos = new ArrayList<Photo>();
    int index = 0;
    final int begin = pagingContext.getStart();
    final int end = begin + pagingContext.getCount();
    final Collection<Photo> dbPhotos = _db.getData().values();
    for (Photo p : dbPhotos)
    {
      if (index == end)
      {
        break;
      }
      else if (index >= begin)
      {
        if (title == null || p.getTitle().equalsIgnoreCase(title))
        {
          if (format == null || format == p.getFormat())
          {
            photos.add(p);
          }
        }
      }

      index++;
    }
    return photos;
  }

  @BatchFinder(value = "searchPhotos", batchParam = "criteria")
  public BatchFinderResult<PhotoCriteria, Photo, NoMetadata> searchPhotos(@PagingContextParam PagingContext pagingContext,
      @QueryParam("criteria") PhotoCriteria[] criteria, @QueryParam("exif") @Optional EXIF exif)
  {
     BatchFinderResult<PhotoCriteria, Photo, NoMetadata> batchFinderResult = new BatchFinderResult<>();

    for (PhotoCriteria currentCriteria: criteria) {
      if (currentCriteria.getTitle() != null) {
        // on success
        final List<Photo> photos = new ArrayList<Photo>();
        int index = 0;
        final int begin = pagingContext.getStart();
        final int end = begin + pagingContext.getCount();
        final Collection<Photo> dbPhotos = _db.getData().values();
        for (Photo p : dbPhotos)
        {
          if (index == end)
          {
            break;
          }
          else if (index >= begin)
          {
            if (p.getTitle().equalsIgnoreCase(currentCriteria.getTitle()))
            {
              if (currentCriteria.getFormat() == null || currentCriteria.getFormat() == p.getFormat())
              {
                photos.add(p);
              }
            }
          }

          index++;
        }
        CollectionResult<Photo, NoMetadata> cr = new CollectionResult<>(photos, photos.size());
        batchFinderResult.putResult(currentCriteria, cr);
      } else {
        // on error: to construct error response for test
        batchFinderResult.putError(currentCriteria, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "Failed to find Photo!"));
      }
    }

    return batchFinderResult;
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

  public final static String URN_ENTITY_TYPE = "photo";

  // use dependency injection instead of hard-coding database instance
  // photo-server-cmpt will define a bean with the same name as declared here ("photoDb"),
  // which points to an implementation of the PhotoDatabase interface
  @Inject
  @Named("photoDb")
  private PhotoDatabase      _db;

  // need this to cascade deletes
  @Inject
  @Named("albumEntryDb")
  private AlbumEntryDatabase _entryDb;
}
