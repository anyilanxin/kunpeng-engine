package com.anyilanxin.modules.common.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstancePageDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceQueryDto;
import com.anyilanxin.modules.common.entity.ActivityInstanceEntity;
import com.anyilanxin.modules.common.mapper.ActivityInstanceMapper;
import com.anyilanxin.modules.common.service.IActivityInstanceService;
import com.anyilanxin.modules.common.service.vo.ActivityInstancePageVo;
import com.anyilanxin.modules.common.service.vo.ActivityInstanceVo;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 活动实例信息(ActivityInstance)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ActivityInstanceServiceImpl
    extends ServiceImpl<ActivityInstanceMapper, ActivityInstanceEntity>
    implements IActivityInstanceService {
  private final Converter converter;
  private final ActivityInstanceMapper mapper;

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final ActivityInstanceDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, ActivityInstanceEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final String activityInstanceId, final ActivityInstanceDto dto)
      throws RuntimeException {
    // 查询数据是否存在
    getById(activityInstanceId);
    // 更新数据
    final var entity = converter.convert(dto, ActivityInstanceEntity.class);
    entity.setActivityInstanceId(activityInstanceId);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<ActivityInstanceVo> selectListByModel(final ActivityInstanceQueryDto dto)
      throws RuntimeException {
    final var list = mapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<ActivityInstancePageVo> pageByModel(final ActivityInstancePageDto dto)
      throws RuntimeException {
    return toPageData(mapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public ActivityInstanceVo getById(final String activityInstanceId) throws RuntimeException {
    final var byId = super.getById(activityInstanceId);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, ActivityInstanceVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final String activityInstanceId) throws RuntimeException {
    // 查询数据是否存在
    getById(activityInstanceId);
    // 删除数据
    final var b = removeById(activityInstanceId);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<String> activityInstanceIds) throws RuntimeException {
    final var entities = listByIds(activityInstanceIds);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<String>();
    entities.forEach(v -> waitDeleteList.add(v.getActivityInstanceId()));
    final var i = mapper.deleteByIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }
}
