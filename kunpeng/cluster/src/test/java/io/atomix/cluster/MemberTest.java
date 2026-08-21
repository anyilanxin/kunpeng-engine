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
package io.atomix.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.atomix.utils.net.Address;
import java.util.Properties;
import org.junit.jupiter.api.Test;

final class MemberTest {

  private static final Address ADDRESS = Address.from("localhost", 26502);

  @Test
  void shouldPropagateZoneFromMemberIdWhenBuildingZonedMemberViaFactory() {
    // given
    final var memberId = MemberId.from("us-east", 0);

    // when
    final var member = Member.member(memberId, ADDRESS);

    // then
    assertThat(member.zone()).isEqualTo("us-east");
  }

  @Test
  void shouldBuildBareMemberViaFactoryWhenMemberIdHasNoZone() {
    // given
    final var memberId = MemberId.from(0);

    // when / then
    assertThatNoException().isThrownBy(() -> Member.member(memberId, ADDRESS));
    assertThat(Member.member(memberId, ADDRESS).zone()).isNull();
  }

  @Test
  void shouldBuildViaConfigConstructorWhenZoneMatchesMemberId() {
    // given
    final var memberId = MemberId.from("us-east", 0);
    final var config = new MemberConfig().setId(memberId).setZoneId("us-east").setAddress(ADDRESS);

    // when
    final var member = new Member(config);

    // then
    assertThat(member.id()).isEqualTo(memberId);
    assertThat(member.zone()).isEqualTo("us-east");
  }

  @Test
  void shouldThrowViaConfigConstructorWhenZoneDoesNotMatchMemberId() {
    // given
    final var memberId = MemberId.from("us-east", 0);
    final var config = new MemberConfig().setId(memberId).setZoneId("eu-west");

    // when / then
    assertThatThrownBy(() -> new Member(config)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldDeriveZoneFromMemberIdViaTwoArgConstructor() {
    // given
    final var memberId = MemberId.from("us-east", 0);

    // when
    final var member = new Member(memberId, ADDRESS);

    // then
    assertThat(member.id()).isEqualTo(memberId);
    assertThat(member.zone()).isEqualTo("us-east");
  }

  @Test
  void shouldBuildBareMemberViaTwoArgConstructorWhenMemberIdHasNoZone() {
    // given
    final var memberId = MemberId.from(0);

    // when
    final var member = new Member(memberId, ADDRESS);

    // then
    assertThat(member.zone()).isNull();
  }

  @Test
  void shouldSetAllFieldsViaFullConstructorWhenZoneMatchesMemberId() {
    // given
    final var memberId = MemberId.from("us-east", 0);
    final var properties = new Properties();
    properties.setProperty("k", "v");

    // when
    final var member = new Member(memberId, 3L, ADDRESS, "us-east", "rack-1", "host-1", properties);

    // then
    assertThat(member.id()).isEqualTo(memberId);
    assertThat(member.nodeVersion()).isEqualTo(3L);
    assertThat(member.zone()).isEqualTo("us-east");
    assertThat(member.rack()).isEqualTo("rack-1");
    assertThat(member.host()).isEqualTo("host-1");
    assertThat(member.properties()).isEqualTo(properties);
  }

  @Test
  void shouldThrowViaFullConstructorWhenZoneDoesNotMatchMemberId() {
    // given
    final var memberId = MemberId.from("us-east", 0);

    // when / then
    assertThatThrownBy(
            () -> new Member(memberId, 0L, ADDRESS, "eu-west", null, null, new Properties()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
