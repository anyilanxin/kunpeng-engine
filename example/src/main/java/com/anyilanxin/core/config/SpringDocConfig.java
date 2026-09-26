/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.core.config;

import com.anyilanxin.core.config.properties.CoreWebMvcProperty;
import com.anyilanxin.core.config.properties.SpringDocCoreMvcProperty;
import com.anyilanxin.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * spring doc配置
 *
 * @author zxh
 * @date 2021-12-12 14:58
 * @since 1.0.0
 */
@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
public class SpringDocConfig {
  private final SpringDocCoreMvcProperty property;
  private final CoreWebMvcProperty mvcProperty;

  private RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Autowired
  private void setRequestMappingHandlerMapping(
      @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping requestMappingHandlerMapping) {
    this.requestMappingHandlerMapping = requestMappingHandlerMapping;
  }

  private final ApplicationContext applicationContext;

  /**
   * 基本配置
   *
   * @param apiPrefix ${@link String}
   * @return OpenAPI ${@link OpenAPI}
   * @author zxh
   * @date 2021-12-12 16:51
   */
  @Bean
  public OpenAPI customOpenAPI(@Value("${springdoc.api-prefix}") final String apiPrefix) {
    // springdoc安全配置
    final List<SecurityRequirement> security = new ArrayList<>();
    final Components components = new Components();

    // oauth2授权配置
    final SecurityScheme securityScheme = new SecurityScheme();
    final OAuthFlows oAuthFlows = new OAuthFlows();
    final OAuthFlow oAuthFlow =
        new OAuthFlow()
            .authorizationUrl(property.getAuth2Prefix() + "/oauth2/authorize")
            .tokenUrl(property.getAuth2Prefix() + "/oauth2/token");
    oAuthFlows.authorizationCode(oAuthFlow);
    securityScheme.flows(oAuthFlows).type(SecurityScheme.Type.OAUTH2);
    components.addSecuritySchemes(HttpHeaders.AUTHORIZATION, securityScheme);
    security.add(new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION));

    // 其他请求头key
    final Set<String> headers = property.getHeaders();
    headers.forEach(
        v -> {
          components.addSecuritySchemes(
              v,
              new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER));
          security.add(new SecurityRequirement().addList(v));
        });
    int total = 0;
    final Map<RequestMappingInfo, HandlerMethod> handlerMethodMap =
        requestMappingHandlerMapping.getHandlerMethods();
    for (final Map.Entry<RequestMappingInfo, HandlerMethod> infoEntry :
        handlerMethodMap.entrySet()) {
      final HandlerMethod handlerMethod = infoEntry.getValue();
      if (!isEffective(handlerMethod)) {
        continue;
      }
      total++;
    }
    // @formatter:off
    return new OpenAPI()
        .info(
            new Info()
                .title(property.getTitle() + "(总计:" + total + ")")
                .version(property.getVersion()))
        .addServersItem(new Server().url(mvcProperty.getContentPath()))
        .addServersItem(new Server().url(apiPrefix))
        .components(components)
        .security(security);
    // @formatter:off
  }

  /**
   * 默认分组
   *
   * @return GroupedOpenApi ${@link GroupedOpenApi}
   * @author zxh
   * @date 2021-12-12 16:51
   */
  @Bean
  public GroupedOpenApi defaultGroup() {
    // @formatter:off
    // 注册其他group
    registerOtherGrouped();
    // 注册全量group
    return GroupedOpenApi.builder()
        .group("all")
        .packagesToScan(property.getPackagesToScan().split("[,，]"))
        .build();
    // @formatter:off
  }

  @Bean
  public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
    return openApi ->
        openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(
                operation -> {
                  try {
                    final HttpServletRequest request = ServletUtils.getRequest();
                    System.out.println(
                        "-------customerGlobalHeaderOpenApiCustomizer--------" + request);
                  } catch (final Exception e) {
                    log.info("----e---------{}", e.getMessage());
                  }
                  operation.setTags(List.of("默认信息"));
                });
  }

  /**
   * 其他分组
   *
   * @author zxh
   * @date 2021-12-12 16:51
   */
  public void registerOtherGrouped() {
    final Map<String, DocInfoModel> docInfoModelMap = new HashMap<>(64);
    final Map<RequestMappingInfo, HandlerMethod> handlerMethodMap =
        requestMappingHandlerMapping.getHandlerMethods();
    for (final Map.Entry<RequestMappingInfo, HandlerMethod> infoEntry :
        handlerMethodMap.entrySet()) {
      final HandlerMethod handlerMethod = infoEntry.getValue();
      if (!isEffective(handlerMethod)) {
        continue;
      }
      final Operation annotation = handlerMethod.getMethod().getAnnotation(Operation.class);
      final String[] tags = annotation.tags();
      for (final String tag : tags) {
        final String beanName = tag.replaceAll("\\.", "");
        DocInfoModel docInfoModel = docInfoModelMap.get(beanName);
        if (Objects.isNull(docInfoModel)) {
          docInfoModel = new DocInfoModel();
          docInfoModel.setTotal(1);
          docInfoModel.setVersion(tag);
        } else {
          docInfoModel.setTotal(docInfoModel.getTotal() + 1);
        }
        docInfoModelMap.put(beanName, docInfoModel);
      }
    }
    if (!docInfoModelMap.isEmpty()) {
      final DefaultListableBeanFactory defaultListableBeanFactory =
          (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
      // @formatter:off
      docInfoModelMap.forEach(
          (k, v) -> {
            final GroupedOpenApi build =
                GroupedOpenApi.builder()
                    .group(v.getVersion())
                    .packagesToScan(property.getPackagesToScan().split("[,，]"))
                    .addOpenApiCustomizer(
                        sv ->
                            sv.setInfo(
                                new Info()
                                    .title(property.getTitle() + "(总计:" + v.getTotal() + ")")
                                    .version(v.getVersion())))
                    .addOpenApiMethodFilter(
                        sv -> {
                          final Class<?> beanType = sv.getClass();
                          final Hidden hidden = beanType.getAnnotation(Hidden.class);
                          if (Objects.nonNull(hidden)) {
                            return false;
                          }
                          final Operation annotation = sv.getAnnotation(Operation.class);
                          if (Objects.isNull(annotation)) {
                            return false;
                          }
                          final String[] tagInfos = annotation.tags();
                          if (Objects.isNull(tagInfos) || tagInfos.length <= 0) {
                            return false;
                          }
                          return Arrays.asList(tagInfos).contains(v.getVersion());
                        })
                    .build();
            if (!defaultListableBeanFactory.containsBean(k)) {
              defaultListableBeanFactory.registerSingleton(k, build);
            }
          });
      // @formatter:off
    }
  }

  /** 判断接口是否有效 */
  private boolean isEffective(final HandlerMethod handlerMethod) {
    final Class<?> beanType = handlerMethod.getBeanType();
    final Hidden hidden = beanType.getAnnotation(Hidden.class);
    if (Objects.nonNull(hidden)) {
      return false;
    }
    final Operation annotation = handlerMethod.getMethod().getAnnotation(Operation.class);
    return Objects.nonNull(annotation) && !annotation.hidden();
  }

  /**
   * doc信息
   *
   * @author zxh
   * @date 2022-04-22 00:12
   * @since 1.0.0
   */
  @Getter
  @Setter
  public static class DocInfoModel {
    /** 当前版本总数 */
    private int total;

    /** 版本 */
    private String version;
  }
}
