package com.anyilanxin.modules.variable.service.impl;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.variable.remove.RemoveVariableCommand;
import com.anyilanxin.kunpeng.client.command.variable.update.UpdateVariableCommand;
import com.anyilanxin.modules.variable.controller.dto.VariableRemoveDto;
import com.anyilanxin.modules.variable.controller.dto.VariableUpdateDto;
import com.anyilanxin.modules.variable.service.IVariableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流程实例信息(ProcessInstance)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class VariableServiceImpl implements IVariableService {
  private final KunpengClient client;

  @Override
  public void update(VariableUpdateDto dto) {
    UpdateVariableCommand updateVariableCommand = client.newVariableUpdateCommand();
    if (dto.getActivityInstanceId() != null && dto.getActivityInstanceId() > 0) {
      updateVariableCommand.activityInstanceId(dto.getActivityInstanceId());
    }
    if (dto.getTaskId() != null && dto.getTaskId() > 0) {
      updateVariableCommand.taskId(dto.getTaskId());
    }
    if (dto.getProcessInstanceId() != null && dto.getProcessInstanceId() > 0) {
      updateVariableCommand.processInstanceId(dto.getProcessInstanceId());
    }
  }

  @Override
  public void remove(VariableRemoveDto dto) {
    RemoveVariableCommand removeVariableCommand = client.newVariableRemoveCommand();
    RemoveVariableCommand.RemoveVariableCommandStep1 commandStep1 = null;
    if (dto.getActivityInstanceId() != null && dto.getActivityInstanceId() > 0) {
      commandStep1 = removeVariableCommand.activityInstanceId(dto.getActivityInstanceId());
    }
    if (dto.getTaskId() != null && dto.getTaskId() > 0) {
      commandStep1 = removeVariableCommand.taskId(dto.getTaskId());
    }
    if (dto.getProcessInstanceId() != null && dto.getProcessInstanceId() > 0) {
      commandStep1 = removeVariableCommand.processInstanceId(dto.getProcessInstanceId());
    }
    if (commandStep1 != null) {
      if (dto.isRemoveAll()) {
        commandStep1.removeAll().send().join();
      } else {
        commandStep1.removes(dto.getRemoveVariable()).send().join();
      }
    }
  }
}
