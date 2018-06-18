/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akolb;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.akolb.Util.filterMatches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UtilTest {

  public UtilTest() {
  }

  /**
   * Test that a null pattern returns all candidates for iflterMatches.
   * Also verify that null candidates result in an empty result list.
   */
  @Test
  public void filterMatchesEmpty() {
    List<String> candidates = ImmutableList.of("a", "b");
    assertThat(filterMatches(candidates, null), is(candidates));
    assertThat(filterMatches(null, candidates), is(Collections.emptyList()));
  }

  /**
   * Test positive matches when some candidates match.
   */
  @Test
  public void filterMatchesPositive() {
    List<String> candidates = ImmutableList.of("a", "b");
    List<String> expected = ImmutableList.of("a");
    List<String> filtered = filterMatches(candidates, Collections.singletonList("a"));
    assertThat(filtered, is(expected));
  }

  /**
   * Test matches for negative notation (!something).
   */
  @Test
  public void filterMatchesNegative() {
    List<String> candidates = ImmutableList.of("a", "b");
    List<String> expected = ImmutableList.of("a");
    assertThat(filterMatches(candidates, Collections.singletonList("!b")), is(expected));
  }

  /**
   * Test that multiple patterns are handled correctly. We use one positive and one negative parrent.
   */
  @Test
  public void filterMatchesMultiple() {
    List<String> candidates = ImmutableList.of("a", "b", "any", "boom", "hello");
    List<String> patterns = ImmutableList.of("^a", "!y$");
    List<String> expected = ImmutableList.of("a");
    assertThat(filterMatches(candidates, patterns), is(expected));
  }
}