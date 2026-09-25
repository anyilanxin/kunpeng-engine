/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.spring.jobhandling.parameter;

import com.anyilanxin.kunpeng.client.command.ListenerEventType;
import com.anyilanxin.kunpeng.client.command.UserTaskProperties;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.JobKind;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import java.util.List;
import java.util.Map;

/**
 * 兼容模式的 ActivatedJob 参数解析器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CompatActivatedJobParameterResolver implements ParameterResolver {

  @Override
  public Object resolve(final JobClient jobClient, final ActivatedJob job) {
    return new ActivatedJobProxy(job);
  }

  public static class ActivatedJobProxy implements ActivatedJob {
    private final ActivatedJob job;

    public ActivatedJobProxy(final ActivatedJob job) {
      this.job = job;
    }

    @Override
    public JobKind getKind() {
      return job.getKind();
    }

    @Override
    public ListenerEventType getListenerEventType() {
      return job.getListenerEventType();
    }

    @Override
    public long getKey() {
      return job.getKey();
    }

    @Override
    public String getType() {
      return job.getType();
    }

    @Override
    public long getProcessInstanceId() {
      return job.getProcessInstanceId();
    }

    @Override
    public String getProcessDefinitionKey() {
      return job.getProcessDefinitionKey();
    }

    @Override
    public int getProcessDefinitionVersion() {
      return job.getProcessDefinitionVersion();
    }

    @Override
    public long getProcessDefinitionId() {
      return job.getProcessDefinitionId();
    }

    @Override
    public String getActivityDefinitionKey() {
      return job.getActivityDefinitionKey();
    }

    @Override
    public long getActivityInstanceId() {
      return job.getActivityInstanceId();
    }

    @Override
    public Map<String, String> getCustomHeaders() {
      return job.getCustomHeaders();
    }

    @Override
    public String getWorker() {
      return job.getWorker();
    }

    @Override
    public int getRetries() {
      return job.getRetries();
    }

    @Override
    public long getDeadline() {
      return job.getDeadline();
    }

    @Override
    public String getVariables() {
      return job.getVariables();
    }

    @Override
    public Map<String, Object> getVariablesAsMap() {
      return job.getVariablesAsMap();
    }

    @Override
    public <T> T getVariablesAsType(final Class<T> variableType) {
      return job.getVariablesAsType(variableType);
    }

    @Override
    public <T> T getVariablesAsType(final String name, final Class<T> variableType) {
      return job.getVariablesAsType(name, variableType);
    }

    @Override
    public Object getVariable(final String name) {
      return job.getVariable(name);
    }

    @Override
    public UserTaskProperties getUserTask() {
      if (job.getUserTask() == null) {
        return null;
      }

      return new UserTaskProperties() {
        @Override
        public String getAction() {
          return job.getUserTask().getAction();
        }

        @Override
        public String getAssignee() {
          return job.getUserTask().getAssignee();
        }

        @Override
        public List<String> getCandidateGroups() {
          return job.getUserTask().getCandidateGroups();
        }

        @Override
        public List<String> getCandidateUsers() {
          return job.getUserTask().getCandidateUsers();
        }

        @Override
        public List<String> getChangedAttributes() {
          return job.getUserTask().getChangedAttributes();
        }

        @Override
        public String getDueDate() {
          return job.getUserTask().getDueDate();
        }

        @Override
        public String getFollowUpDate() {
          return job.getUserTask().getFollowUpDate();
        }

        @Override
        public Long getFormKey() {
          return job.getUserTask().getFormKey();
        }

        @Override
        public Integer getPriority() {
          return job.getUserTask().getPriority();
        }

        @Override
        public Long getUserTaskKey() {
          return job.getUserTask().getUserTaskKey();
        }
      };
    }

    @Override
    public String toJson() {
      return job.toJson();
    }

    @Override
    public String getTenantId() {
      return job.getTenantId();
    }
  }
}
