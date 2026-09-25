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
package com.anyilanxin.kunpeng.client.spring.annotation.processor;

import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.getDeploymentValue;
import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.isDeployment;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommand;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommandResponse;
import com.anyilanxin.kunpeng.client.spring.annotation.value.DeploymentValue;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import com.anyilanxin.kunpeng.client.spring.event.KunpengPostDeploymentEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 部署注解处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentAnnotationProcessor extends AbstractKunpengAnnotationProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentAnnotationProcessor.class);

  private static final ResourcePatternResolver RESOURCE_RESOLVER =
      new PathMatchingResourcePatternResolver();

  private final List<DeploymentValue> deploymentValues = new ArrayList<>();
  private final ApplicationEventPublisher publisher;

  public DeploymentAnnotationProcessor(final ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public boolean isApplicableFor(final ClassInfo beanInfo) {
    return isDeployment(beanInfo);
  }

  @Override
  public void configureFor(final ClassInfo beanInfo) {
    final Optional<DeploymentValue> kunpengDeploymentValue = getDeploymentValue(beanInfo);
    if (kunpengDeploymentValue.isPresent()) {
      LOGGER.info("Configuring deployment: {}", kunpengDeploymentValue.get());
      deploymentValues.add(kunpengDeploymentValue.get());
    }
  }

  @Override
  public void start(final KunpengClient client) {
    final List<Resource> resources =
        deploymentValues.stream()
            .flatMap(d -> d.getResources().stream())
            .flatMap(r -> Arrays.stream(getResources(r)))
            .distinct()
            .toList();
    if (resources.isEmpty()) {
      if (deploymentValues.isEmpty()) {
        return;
      }
      throw new IllegalArgumentException("No resources found to deploy");
    }

    final DeployResourceCommand command = client.newDeployResourceCommand();
    DeployResourceCommand.DeployResourceCommandStep2 commandStep2 = null;
    for (final Resource resource : resources) {
      try (final InputStream inputStream = resource.getInputStream()) {
        if (commandStep2 == null) {
          commandStep2 = command.addResourceStream(inputStream, resource.getFilename());
        } else {
          commandStep2 = commandStep2.addResourceStream(inputStream, resource.getFilename());
        }
      } catch (final IOException e) {
        throw new RuntimeException("Error reading resource: " + e.getMessage(), e);
      }
    }
    final DeployResourceCommandResponse deployment = commandStep2.send().join();
    LOGGER.info(
        "Deployed: {}",
        Stream.concat(
                deployment.getDecisionRequirements().stream()
                    .map(
                        wf ->
                            String.format(
                                "<%s:%d>", wf.getDmnDecisionRequirementsId(), wf.getVersion())),
                deployment.getProcessDefinitions().stream()
                    .map(
                        wf ->
                            String.format(
                                "<%s:%d>",
                                wf.getProcessDefinitionId(), wf.getProcessDefinitionVersion())))
            .collect(Collectors.joining(",")));
    publisher.publishEvent(new KunpengPostDeploymentEvent(this, deployment));
  }

  @Override
  public void stop(final KunpengClient client) {
    // noop for deployment
  }

  public Resource[] getResources(final String resources) {
    try {
      return RESOURCE_RESOLVER.getResources(resources);
    } catch (final IOException e) {
      return new Resource[0];
    }
  }
}
