package com.linkedin.jersey.api.uri;

import org.testng.annotations.Test;

public class UriComponentTest {

  @Test
  public void testPathSegmentEquals() {
    UriComponent.PathSegment p1 = new UriComponent.PathSegment("abc", false);
    UriComponent.PathSegment p2 = new UriComponent.PathSegment("def", false);
    UriComponent.PathSegment p3 = new UriComponent.PathSegment("abc", false);
    UriComponent.PathSegment p4 = new UriComponent.PathSegment("abc?x=a%20b", true);
    assert(p1.equals(p3));
    assert(!p1.equals(p4));
    assert(!p1.equals(p2));

    String expectedToString = "PathSegment[path='abc', matrixParameters={}]";
    assert(expectedToString.equals(p1.toString()));
    assert(expectedToString.equals(p1.toString()));

    String expectedToString2 = "PathSegment[path='abc?x=a b', matrixParameters={}]";
    assert(expectedToString2.equals(p4.toString()));
    assert(expectedToString2.equals(p4.toString()));
  }
}