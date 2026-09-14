/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import java.util.*;

public class DmnDecisionResultImpl implements DmnDecisionResult {

  private static final long serialVersionUID = 1L;

  public static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final List<DmnDecisionResultEntries> ruleResults;

  public DmnDecisionResultImpl(final List<DmnDecisionResultEntries> ruleResults) {
    this.ruleResults = ruleResults;
  }

  @Override
  public DmnDecisionResultEntries getFirstResult() {
    if (size() > 0) {
      return get(0);
    } else {
      return null;
    }
  }

  @Override
  public DmnDecisionResultEntries getSingleResult() {
    if (size() == 1) {
      return get(0);
    } else if (isEmpty()) {
      return null;
    } else {
      throw LOG.decisionResultHasMoreThanOneOutput(this);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> collectEntries(final String outputName) {
    final List<T> outputValues = new ArrayList<T>();

    for (final DmnDecisionResultEntries ruleResult : ruleResults) {
      if (ruleResult.containsKey(outputName)) {
        final Object value = ruleResult.get(outputName);
        outputValues.add((T) value);
      }
    }

    return outputValues;
  }

  @Override
  public List<Map<String, Object>> getResultList() {
    final List<Map<String, Object>> entryMapList = new ArrayList<Map<String, Object>>();

    for (final DmnDecisionResultEntries ruleResult : ruleResults) {
      final Map<String, Object> entryMap = ruleResult.getEntryMap();
      entryMapList.add(entryMap);
    }

    return entryMapList;
  }

  @Override
  public <T> T getSingleEntry() {
    final DmnDecisionResultEntries result = getSingleResult();
    if (result != null) {
      return result.getSingleEntry();
    } else {
      return null;
    }
  }

  @Override
  public <T extends TypedValue> T getSingleEntryTyped() {
    final DmnDecisionResultEntries result = getSingleResult();
    if (result != null) {
      return result.getSingleEntryTyped();
    } else {
      return null;
    }
  }

  @Override
  public Iterator<DmnDecisionResultEntries> iterator() {
    return asUnmodifiableList().iterator();
  }

  @Override
  public int size() {
    return ruleResults.size();
  }

  @Override
  public boolean isEmpty() {
    return ruleResults.isEmpty();
  }

  @Override
  public DmnDecisionResultEntries get(final int index) {
    return ruleResults.get(index);
  }

  @Override
  public boolean contains(final Object o) {
    return ruleResults.contains(o);
  }

  @Override
  public Object[] toArray() {
    return ruleResults.toArray();
  }

  @Override
  public <T> T[] toArray(final T[] a) {
    return ruleResults.toArray(a);
  }

  @Override
  public boolean add(final DmnDecisionResultEntries e) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public boolean remove(final Object o) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public boolean containsAll(final Collection<?> c) {
    return ruleResults.containsAll(c);
  }

  @Override
  public boolean addAll(final Collection<? extends DmnDecisionResultEntries> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends DmnDecisionResultEntries> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public boolean removeAll(final Collection<?> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public boolean retainAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public DmnDecisionResultEntries set(final int index, final DmnDecisionResultEntries element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public void add(final int index, final DmnDecisionResultEntries element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public DmnDecisionResultEntries remove(final int index) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

  @Override
  public int indexOf(final Object o) {
    return ruleResults.indexOf(o);
  }

  @Override
  public int lastIndexOf(final Object o) {
    return ruleResults.lastIndexOf(o);
  }

  @Override
  public ListIterator<DmnDecisionResultEntries> listIterator() {
    return asUnmodifiableList().listIterator();
  }

  @Override
  public ListIterator<DmnDecisionResultEntries> listIterator(final int index) {
    return asUnmodifiableList().listIterator(index);
  }

  @Override
  public List<DmnDecisionResultEntries> subList(final int fromIndex, final int toIndex) {
    return asUnmodifiableList().subList(fromIndex, toIndex);
  }

  @Override
  public String toString() {
    return ruleResults.toString();
  }

  protected List<DmnDecisionResultEntries> asUnmodifiableList() {
    return Collections.unmodifiableList(ruleResults);
  }
}
