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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedInput;
import java.util.Objects;

public class DmnEvaluatedInputImpl implements DmnEvaluatedInput {

  protected String id;
  protected String name;
  protected String inputVariable;
  protected Object value;

  public DmnEvaluatedInputImpl(final DmnDecisionTableInputImpl input) {
    id = input.getKey();
    name = input.getName();
    inputVariable = input.getInputVariable();
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public String getInputVariable() {
    return inputVariable;
  }

  public void setInputVariable(final String inputVariable) {
    this.inputVariable = inputVariable;
  }

  @Override
  public Object getValue() {
    return value;
  }

  public void setValue(final Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final DmnEvaluatedInputImpl that = (DmnEvaluatedInputImpl) o;

    if (!Objects.equals(id, that.id)) {
      return false;
    }
    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (!Objects.equals(inputVariable, that.inputVariable)) {
      return false;
    }
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (inputVariable != null ? inputVariable.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DmnEvaluatedInputImpl{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", inputVariable='"
        + inputVariable
        + '\''
        + ", value="
        + value
        + '}';
  }
}
