/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FullTextSearchQueryTest {

  @Test
  public void singleTermQueryTest() {
    assertEquals("test:*", fromGoogleLikeQuery("test"));
  }

  @Test
  public void singleTermWithSpecialCharactersQueryTest() {
    assertEquals("test:*", fromGoogleLikeQuery("test:<&"));
  }

  @Test
  public void singleTermWithSpacesQueryTest() {
    assertEquals("test:*", fromGoogleLikeQuery("   test   "));
  }

  @Test
  public void doubleTermQueryTest() {
    assertEquals("test1:* & test2:*", fromGoogleLikeQuery("test1 test2"));
  }

  @Test
  public void doubleTermWithSpacesQueryTest() {
    assertEquals("test1:* & test2:*", fromGoogleLikeQuery("   test1       test2   "));
  }

  @Test
  public void exactTermQueryTest() {
    assertEquals("test1 <-> test2", fromGoogleLikeQuery("\"test1 test2\""));
  }

  @Test
  public void exactTermWithSpacesQueryTest() {
    assertEquals("test1 <-> test2", fromGoogleLikeQuery("   \"   test1       test2   \"   "));
  }

  @Test
  public void exactAndBetweenTermsQueryTest() {
    assertEquals("test1:* & test2 <-> test3 & test4:*",
        fromGoogleLikeQuery("test1 \"test2 test3\" test4"));
  }

  @Test
  public void unterminatedExactTermQueryTest() {
    assertEquals("test1 <-> test2", fromGoogleLikeQuery("\"test1 test2"));
  }

  @Test
  public void unstartedExactTermQueryTest() {
    assertEquals("test1:* & test2:*", fromGoogleLikeQuery("test1 test2\""));
  }

  private String fromGoogleLikeQuery(String query) {
    return new FullTextSearchQuery(query).getFullTextSearchQuery();
  }

}
