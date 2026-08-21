/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.raft.protocol;

import io.atomix.raft.RaftError;

/** 节点加入集群请求（{@link JoinRequest}）的应答消息。 */
public final class JoinResponse extends AbstractRaftResponse {

  /** {@link JoinResponse} 的构建器，字段状态直接由本类持有并组装。 */
  public static final class Builder extends AbstractRaftResponse.Builder<Builder, JoinResponse> {

    private Builder() {}

    /**
     * 组装应答对象。
     *
     * <p>先做前置校验（status 必填），校验通过后把构建器自身的字段快照传入构造器完成组装。
     */
    @Override
    public JoinResponse build() {
      // 卫语句：状态未设置时直接失败，避免构造出非法应答
      if (status == null) {
        throw new NullPointerException("status cannot be null");
      }
      return new JoinResponse(this);
    }
  }

  /** 入口工厂，返回一个新的构建器。 */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 私有构造器，从构建器快照中取值，不暴露独立的字段参数。
   *
   * @param source 已通过校验的构建器
   */
  private JoinResponse(final Builder source) {
    super(source.status, source.error);
  }

  /** 应答状态，透传自基类持有的字段。 */
  @Override
  public Status status() {
    return super.status();
  }

  /** 失败时的错误信息，透传自基类持有的字段。 */
  @Override
  public RaftError error() {
    return super.error();
  }
}
