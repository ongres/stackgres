/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class FullTextSearchQueryTest {

  @Test
  public void emptyQueryTest() {
    assertEquals(Optional.empty(), fromGoogleLikeQuery(""));
  }

  @Test
  public void emptyWithSpacesQueryTest() {
    assertEquals(Optional.empty(), fromGoogleLikeQuery("   "));
  }

  @Test
  public void singleTermQueryTest() {
    assertEquals(Optional.of("test:*"), fromGoogleLikeQuery("test"));
  }

  @Test
  public void singleTermWithSpecialCharactersQueryTest() {
    assertEquals(Optional.of("test:*"), fromGoogleLikeQuery("test:<&"));
  }

  @Test
  public void singleTermWithSpacesQueryTest() {
    assertEquals(Optional.of("test:*"), fromGoogleLikeQuery("   test   "));
  }

  @Test
  public void doubleTermQueryTest() {
    assertEquals(Optional.of("test1:* & test2:*"), fromGoogleLikeQuery("test1 test2"));
  }

  @Test
  public void doubleTermWithSpacesQueryTest() {
    assertEquals(Optional.of("test1:* & test2:*"), fromGoogleLikeQuery("   test1       test2   "));
  }

  @Test
  public void exactTermQueryTest() {
    assertEquals(Optional.of("test1 <-> test2"), fromGoogleLikeQuery("\"test1 test2\""));
  }

  @Test
  public void exactTermWithSpacesQueryTest() {
    assertEquals(Optional.of("test1 <-> test2"),
        fromGoogleLikeQuery("   \"   test1       test2   \"   "));
  }

  @Test
  public void exactAndBetweenTermsQueryTest() {
    assertEquals(Optional.of("test1:* & test2 <-> test3 & test4:*"),
        fromGoogleLikeQuery("test1 \"test2 test3\" test4"));
  }

  @Test
  public void unterminatedExactTermQueryTest() {
    assertEquals(Optional.of("test1 <-> test2"), fromGoogleLikeQuery("\"test1 test2"));
  }

  @Test
  public void unstartedExactTermQueryTest() {
    assertEquals(Optional.of("test1:* & test2:*"), fromGoogleLikeQuery("test1 test2\""));
  }

  private Optional<String> fromGoogleLikeQuery(String query) {
    return new FullTextSearchQuery(query).getFullTextSearchQuery();
  }

}
