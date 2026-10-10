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
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import java.util.List;
import org.apache.ibatis.session.SqlSession;

/**
 * 一个会话上全部实体映射器的集合：一次 flush / 一次清理只取一份，避免逐行 getMapper。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record SinkMappers(
    ProcessDefinitionMapper processDefinition,
    ProcessInstanceMapper processInstance,
    ActivityInstanceMapper activityInstance,
    VariableMapper variable,
    UserTaskMapper userTask,
    JobMapper job,
    IncidentMapper incident,
    TimerMapper timer,
    MessageSubscriptionMapper messageSubscription,
    SignalSubscriptionMapper signalSubscription,
    SinkPositionMapper sinkPosition) {

  /**
   * @return 全部映射器接口，注册与加载 XML 时使用
   */
  public static List<Class<?>> mapperInterfaces() {
    return List.of(
        ProcessDefinitionMapper.class,
        ProcessInstanceMapper.class,
        ActivityInstanceMapper.class,
        VariableMapper.class,
        UserTaskMapper.class,
        JobMapper.class,
        IncidentMapper.class,
        TimerMapper.class,
        MessageSubscriptionMapper.class,
        SignalSubscriptionMapper.class,
        SinkPositionMapper.class);
  }

  /**
   * @return 映射器接口对应的 XML classpath 路径（约定：mapper/<简单名>.xml）
   */
  public static String mapperResource(final Class<?> mapperInterface) {
    return "mapper/" + mapperInterface.getSimpleName() + ".xml";
  }

  /** 从会话解析全部映射器。 */
  public static SinkMappers from(final SqlSession session) {
    return new SinkMappers(
        session.getMapper(ProcessDefinitionMapper.class),
        session.getMapper(ProcessInstanceMapper.class),
        session.getMapper(ActivityInstanceMapper.class),
        session.getMapper(VariableMapper.class),
        session.getMapper(UserTaskMapper.class),
        session.getMapper(JobMapper.class),
        session.getMapper(IncidentMapper.class),
        session.getMapper(TimerMapper.class),
        session.getMapper(MessageSubscriptionMapper.class),
        session.getMapper(SignalSubscriptionMapper.class),
        session.getMapper(SinkPositionMapper.class));
  }
}
