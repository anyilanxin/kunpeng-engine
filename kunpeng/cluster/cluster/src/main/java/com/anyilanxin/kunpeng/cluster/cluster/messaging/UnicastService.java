/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Service for unreliable unicast messaging between nodes.
 *
 * <p>The unicast service is an unreliable unicast messaging service backed by UDP. This service
 * provides no guarantee regarding reliability or order of messages.
 */
public interface UnicastService {

  /**
   * Sends a unicast message to the given address for the given subject.
   *
   * <p>The message will be unicast to the listener for the given {@code subject}. This service
   * makes no guarantee regarding the reliability or order of delivery of the message.
   *
   * @param address the address to which to unicast the message
   * @param subject the message subject
   * @param message the message to unicast
   */
  void unicast(Address address, String subject, byte[] message);

  /**
   * Adds a unicast listener for the given subject.
   *
   * <p>Messages unicast to the given {@code subject} will be delivered to the provided listener.
   * This service provides no guarantee regarding the order in which messages arrive.
   *
   * @param subject the message subject
   * @param listener the unicast listener to add
   */
  default void addListener(final String subject, final BiConsumer<Address, byte[]> listener) {
    addListener(subject, listener, MoreExecutors.directExecutor());
  }

  /**
   * Adds a unicast listener for the given subject.
   *
   * <p>Messages unicast to the given {@code subject} will be delivered to the provided listener.
   * This service provides no guarantee regarding the order in which messages arrive.
   *
   * @param subject the message subject
   * @param listener the unicast listener to add
   * @param executor an executor with which to call the listener
   */
  void addListener(String subject, BiConsumer<Address, byte[]> listener, Executor executor);

  /**
   * Removes a unicast listener for the given subject.
   *
   * @param subject the message subject
   * @param listener the unicast listener to remove
   */
  void removeListener(String subject, BiConsumer<Address, byte[]> listener);
}
