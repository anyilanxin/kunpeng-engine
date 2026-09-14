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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;

public class DmnEvaluatedOutputImpl implements DmnEvaluatedOutput {

  protected String id;
  protected String name;
  protected String outputName;
  protected TypedValue value;

  public DmnEvaluatedOutputImpl(
      final DmnDecisionTableOutputImpl decisionTableOutput, final TypedValue value) {
    id = decisionTableOutput.getKey();
    name = decisionTableOutput.getName();
    outputName = decisionTableOutput.getOutputName();
    this.value = value;
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
  public String getOutputName() {
    return outputName;
  }

  public void setOutputName(final String outputName) {
    this.outputName = outputName;
  }

  @Override
  public TypedValue getValue() {
    return value;
  }

  public void setValue(final TypedValue value) {
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

    final DmnEvaluatedOutputImpl that = (DmnEvaluatedOutputImpl) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (outputName != null ? !outputName.equals(that.outputName) : that.outputName != null) {
      return false;
    }
    return !(value != null ? !value.equals(that.value) : that.value != null);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (outputName != null ? outputName.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DmnEvaluatedOutputImpl{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", outputName='"
        + outputName
        + '\''
        + ", value="
        + value
        + '}';
  }
}
