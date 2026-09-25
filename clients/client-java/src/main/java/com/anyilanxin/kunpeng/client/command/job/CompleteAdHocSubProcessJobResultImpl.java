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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.ArgumentUtil;
import com.anyilanxin.kunpeng.client.command.CommandWithVariables2;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import java.util.ArrayList;
import java.util.List;

public class CompleteAdHocSubProcessJobResultImpl
    extends CommandWithVariables2<
        CompleteAdHocSubProcessResultStep1.CompleteAdHocSubProcessResultStep2>
    implements CompleteAdHocSubProcessResultStep1,
        CompleteAdHocSubProcessResultStep1.CompleteAdHocSubProcessResultStep2 {

  private final List<ActivateElement> activateElements = new ArrayList<>();
  private ActivateElement latestActivateElement;
  private boolean completionConditionFulfilled;
  private boolean cancelRemainingInstances;

  public CompleteAdHocSubProcessJobResultImpl(final JsonMapper jsonMapper) {
    super(jsonMapper);
  }

  public List<ActivateElement> getActivateElements() {
    return activateElements;
  }

  @Override
  public JobResultType getType() {
    return JobResultType.AD_HOC_SUB_PROCESS;
  }

  @Override
  public CompleteAdHocSubProcessResultStep2 activateElement(final String elementId) {
    ArgumentUtil.ensureNotNull("elementId", elementId);
    latestActivateElement = new ActivateElement().setElementId(elementId);
    activateElements.add(latestActivateElement);
    return this;
  }

  @Override
  public CompleteAdHocSubProcessResultStep1 completionConditionFulfilled(
      final boolean completionConditionFulfilled) {
    this.completionConditionFulfilled = completionConditionFulfilled;
    return this;
  }

  @Override
  public CompleteAdHocSubProcessResultStep1 cancelRemainingInstances(
      final boolean cancelRemainingInstances) {
    this.cancelRemainingInstances = cancelRemainingInstances;
    return this;
  }

  public boolean isCompletionConditionFulfilled() {
    return completionConditionFulfilled;
  }

  public boolean isCancelRemainingInstances() {
    return cancelRemainingInstances;
  }

  @Override
  protected CompleteAdHocSubProcessResultStep2 setVariablesInternal(final String variables) {
    latestActivateElement.setVariables(variables);
    return this;
  }

  public static class ActivateElement {
    private String elementId;
    private String variables;

    public String getElementId() {
      return elementId;
    }

    public ActivateElement setElementId(final String elementId) {
      this.elementId = elementId;
      return this;
    }

    public String getVariables() {
      return variables;
    }

    public ActivateElement setVariables(final String variables) {
      this.variables = variables;
      return this;
    }
  }
}
