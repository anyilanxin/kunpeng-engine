package com.anyilanxin.modules.common.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionPageDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionQueryDto;
import com.anyilanxin.modules.common.entity.ExporterPositionEntity;
import com.anyilanxin.modules.common.mapper.ExporterPositionMapper;
import com.anyilanxin.modules.common.service.IExporterPositionService;
import com.anyilanxin.modules.common.service.vo.ExporterPositionPageVo;
import com.anyilanxin.modules.common.service.vo.ExporterPositionVo;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 导出记录(ExporterPosition)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ExporterPositionServiceImpl
    extends ServiceImpl<ExporterPositionMapper, ExporterPositionEntity>
    implements IExporterPositionService {
  private final Converter converter;
  private final ExporterPositionMapper mapper;

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final ExporterPositionDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, ExporterPositionEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final BigDecimal partitionId, final ExporterPositionDto dto)
      throws RuntimeException {
    // 查询数据是否存在
    getById(partitionId);
    // 更新数据
    final var entity = converter.convert(dto, ExporterPositionEntity.class);
    entity.setPartitionId(partitionId);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<ExporterPositionVo> selectListByModel(final ExporterPositionQueryDto dto)
      throws RuntimeException {
    final var list = mapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<ExporterPositionPageVo> pageByModel(final ExporterPositionPageDto dto)
      throws RuntimeException {
    return toPageData(mapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public ExporterPositionVo getById(final BigDecimal partitionId) throws RuntimeException {
    final var byId = super.getById(partitionId);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, ExporterPositionVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final BigDecimal partitionId) throws RuntimeException {
    // 查询数据是否存在
    getById(partitionId);
    // 删除数据
    final var b = removeById(partitionId);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<BigDecimal> partitionIds) throws RuntimeException {
    final var entities = listByIds(partitionIds);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<BigDecimal>();
    entities.forEach(v -> waitDeleteList.add(v.getPartitionId()));
    final var i = mapper.deleteByIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }
}
