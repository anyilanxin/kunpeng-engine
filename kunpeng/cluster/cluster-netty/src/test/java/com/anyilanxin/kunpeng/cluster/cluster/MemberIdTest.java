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
package com.anyilanxin.kunpeng.cluster.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class MemberIdTest {

  @Test
  void shouldExtractZoneFromZonedForm() {
    // given / when
    final var memberId = MemberId.from("us-east@7");

    // then
    assertThat(memberId)
        .returns("us-east", MemberId::zone)
        .returns("us-east@7", MemberId::id);
    assertThat(memberId.isBare()).isFalse();
    assertEncodeDecode(memberId);
  }

  @Test
  void shouldTreatBareIdAsNoZone() {
    // given / when
    final var memberId = MemberId.from("7");

    // then
    assertThat(memberId).returns(null, MemberId::zone).returns("7", MemberId::id);
    assertThat(memberId.isBare()).isTrue();
    assertEncodeDecode(memberId);
  }

  @Test
  void shouldTreatLeadingSeparatorAsBareId() {
    // given — '@' 在开头时分隔符位置不大于 0，按裸 id 处理
    // when / then
    assertThat(MemberId.from("@7")).returns(null, MemberId::zone);
  }

  @Test
  void shouldThrowWhenZoneContainsSeparator() {
    // given / when / then — zone 内不允许出现 '@'
    assertThatThrownBy(() -> MemberId.from("eu@west@7"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldNotThrowForAnonymous() {
    // given / when / then
    assertThatNoException().isThrownBy(MemberId::anonymous);
    assertThat(MemberId.anonymous()).returns(null, MemberId::zone);
  }

  static Stream<Arguments> isInZoneCases() {
    return Stream.of(
        Arguments.of("us-east@7", "us-east", true), // matching zone
        Arguments.of("us-east@7", "eu-west", false), // different zone
        Arguments.of("7", "us-east", false), // zone set but id is bare
        Arguments.of("7", null, true), // null zone, bare id
        Arguments.of("us-east@1", null, false) // null zone, zoned id
        );
  }

  @ParameterizedTest
  @MethodSource("isInZoneCases")
  void shouldCheckIsInZone(final String id, final String zone, final boolean expected) {
    // given / when / then
    assertThat(MemberId.from(id).isInZone(zone)).isEqualTo(expected);
  }

  @Test
  void shouldThrowWhenMemberZoneDoesNotMatchMemberIdPrefix() {
    // given / then
    assertThatThrownBy(() -> Member.builder(MemberId.from("us-east@0")).withZoneId("us").build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void assertEncodeDecode(final MemberId memberId) {
    final var decoded = MemberId.from(memberId.id());
    assertThat(decoded).isEqualTo(memberId).returns(memberId.hashCode(), MemberId::hashCode);
  }
}
