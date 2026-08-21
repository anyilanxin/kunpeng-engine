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

/** Acknowledgement of a {@link LeaveRequest}. */
public final class LeaveResponse extends AbstractRaftResponse {

  private LeaveResponse(final Status status, final RaftError error) {
    super(status, error);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends AbstractRaftResponse.Builder<Builder, LeaveResponse> {
    private Builder() {}

    @Override
    public LeaveResponse build() {
      validate();
      return new LeaveResponse(status, error);
    }
  }
}
