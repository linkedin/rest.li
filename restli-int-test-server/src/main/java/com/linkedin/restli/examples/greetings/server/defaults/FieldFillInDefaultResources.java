package com.linkedin.restli.examples.greetings.server.defaults;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.defaults.api.HighLevelRecordWithDefault;
import com.linkedin.restli.examples.defaults.api.RecordCriteria;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.util.resources.cldr.ha.CalendarData_ha_Latn_GH;


@RestLiCollection(name = "fillInDefaults", namespace = "com.linkedin.restli.examples.defaults.api")
public class FieldFillInDefaultResources extends CollectionResourceTemplate<Long, HighLevelRecordWithDefault>
{
  @Override
  public HighLevelRecordWithDefault get(Long keyId)
  {
    return new HighLevelRecordWithDefault().setNoDefaultFieldA(Math.toIntExact(keyId));
  }

  @Override
  public Map<Long, HighLevelRecordWithDefault> batchGet(Set<Long> ids)
  {
    Map<Long, HighLevelRecordWithDefault> result = new HashMap<>();
    for (Long id : ids)
    {
      result.put(id, new HighLevelRecordWithDefault().setNoDefaultFieldA(Math.toIntExact(id)));
    }
    return result;
  }

  @Override
  public List<HighLevelRecordWithDefault> getAll(@PagingContextParam PagingContext pagingContext)
  {
    List<HighLevelRecordWithDefault> result = new LinkedList<>();
    for (int i = 0; i < 3; i++)
    {
      result.add(new HighLevelRecordWithDefault().setNoDefaultFieldA(i));
    }
    return result;
  }

  @Finder("HighLevelRecord")
  public List<HighLevelRecordWithDefault> findHighLevelRecord(@QueryParam("totalCount") Integer totalCount)
  {
    List<HighLevelRecordWithDefault> ordersCollection = new ArrayList<>();
    for (int i = 0; i < totalCount; i++)
    {
      ordersCollection.add(new HighLevelRecordWithDefault().setNoDefaultFieldA(i));
    }
    return ordersCollection;
  }

  @BatchFinder(value = "searchRecords", batchParam = "criteria")
  public BatchFinderResult<RecordCriteria, HighLevelRecordWithDefault, Empty> searchRecords(
      @QueryParam("criteria") RecordCriteria[] criteria)
  {

    BatchFinderResult<RecordCriteria, HighLevelRecordWithDefault, Empty> result = new BatchFinderResult<RecordCriteria,
        HighLevelRecordWithDefault, Empty>();
    for (int i = 0; i < criteria.length; i++)
    {
      List<HighLevelRecordWithDefault> currentCriteriaResult = Collections.singletonList(
          new HighLevelRecordWithDefault().setNoDefaultFieldA(criteria[i].getIntWithoutDefault()));
      CollectionResult<HighLevelRecordWithDefault, Empty> cr = new CollectionResult<HighLevelRecordWithDefault, Empty>(
          currentCriteriaResult, currentCriteriaResult.size());
      result.putResult(criteria[i], cr);
    }
    return result;
  }

  @Action(name = "defaultFillAction")
  public ActionResult<HighLevelRecordWithDefault> takeAction(@ActionParam("actionParam") Long id)
  {
    return new ActionResult<HighLevelRecordWithDefault>(new HighLevelRecordWithDefault().setNoDefaultFieldA(
        Math.toIntExact(id)),
        HttpStatus.S_200_OK);
  }
}
