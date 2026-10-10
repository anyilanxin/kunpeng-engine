/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.core.handler;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiCoreCommonUtils;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

/**
 * 异常处理器
 *
 * @author zxh
 * @date 2020-06-22 17:34
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends AnYiBaseController {
  /**
   * 处理所有不可知的异常
   *
   * @param e ${@link Exception}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:21
   */
  @ExceptionHandler(Exception.class)
  public Object handleException(
      final HttpServletRequest request, final HttpServletResponse response, final Exception e) {
    e.printStackTrace();
    log.error(
        "------------GlobalExceptionHandler------处理所有不可知的异常------>handleException--->异常消息:\n{}",
        e.getMessage());
    final Throwable cause = e.getCause();
    if (Objects.nonNull(cause)) {
      if (cause instanceof AnYiResponseException) {
        final AnYiResponseException exception = (AnYiResponseException) cause;
        final AnYiResult<Object> result = exception.getResult();
        return fail(result.getCode(), result.getMessage());
      } else if (Objects.nonNull(cause.getCause())
          && cause.getCause() instanceof AnYiResponseException) {
        final AnYiResponseException exception = (AnYiResponseException) cause.getCause();
        final AnYiResult<Object> result = exception.getResult();
        return fail(result.getCode(), result.getMessage());
      }
      final String str = AnYiCoreCommonUtils.getStackTrace(e);
      if (StringUtils.isNotBlank(str)) {
        return fail(str);
      }
    }
    response.setStatus(HttpStatus.OK.value());
    return fail("服务器出问题了:" + e.getMessage());
  }

  /**
   * 处理自定义异常
   *
   * @param e ${@link AnYiResponseException} 处理异常
   * @return AnYiResult ${@link AnYiResult} 响应前端
   * @author zxh
   * @date 2020-08-27 15:17
   */
  @ExceptionHandler(AnYiResponseException.class)
  @ResponseStatus(HttpStatus.OK)
  public AnYiResult<String> handlerResponseException(final AnYiResponseException e) {
    e.printStackTrace();
    final AnYiResult<Object> result = e.getResult();
    log.error(
        "------------GlobalExceptionHandler------处理自定义异常------>handlerResponseException:\n{}",
        e.getMessage());
    return fail(result.getCode(), result.getMessage());
  }

  /**
   * 处理请求参数校验(普通传参)异常
   *
   * @param e ${@link BindException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2019-06-18 09:35
   */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.OK)
  AnYiResult<String> handleBindException(final BindException e) {
    final StringBuilder sb = new StringBuilder();
    for (final ObjectError error : e.getAllErrors()) {
      final String defaultMessage = error.getDefaultMessage();
      sb.append(",").append(defaultMessage);
    }
    final String errMeg = sb.toString().replaceFirst(",", "");
    log.error(
        "------------GlobalExceptionHandler------处理请求参数校验(普通传参)异常------>handleBindException:\n{}",
        errMeg);
    return fail(AnYiResultStatus.VERIFICATION_FAILED, errMeg);
  }

  /**
   * 处理请求参数校验(普通传参)异常
   *
   * @param e ${@link ConstraintViolationException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2019-06-18 09:35
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.OK)
  AnYiResult<String> handleConstraintViolationException(final ConstraintViolationException e) {
    e.printStackTrace();
    final StringBuilder sb = new StringBuilder();
    final Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
    for (final ConstraintViolation<?> violation : violations) {
      final String defaultMessage = violation.getMessage();
      sb.append(",").append(defaultMessage);
    }
    final String errMeg = sb.toString().replaceFirst(",", "");
    log.error(
        "------------GlobalExceptionHandler------处理请求参数校验(普通传参)异常------>handleConstraintViolationException:\n{}",
        errMeg);
    return fail(AnYiResultStatus.VERIFICATION_FAILED, errMeg);
  }

  /**
   * 处理请求参数校验(实体对象传参)异常
   *
   * @param e ${@link MethodArgumentNotValidException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:27
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.OK)
  public AnYiResult<String> handleMethodArgumentNotValidException(
      final MethodArgumentNotValidException e) {
    e.printStackTrace();
    final StringBuilder sb = new StringBuilder();
    for (final ObjectError error : e.getAllErrors()) {
      final String defaultMessage = error.getDefaultMessage();
      sb.append(",").append(defaultMessage);
    }
    final String errMeg = sb.toString().replaceFirst(",", "");
    log.error(
        "------------GlobalExceptionHandler------处理请求参数校验(实体对象传参)异常------>handleMethodArgumentNotValidException:\n{}",
        errMeg);
    return fail(AnYiResultStatus.VERIFICATION_FAILED, errMeg);
  }

  /**
   * 处理数据库数据重复异常
   *
   * @param e ${@link DuplicateKeyException}
   * @return AnYiResult${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:27
   */
  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.OK)
  public AnYiResult<String> handleDuplicateKeyException(final DuplicateKeyException e) {
    e.printStackTrace();
    String errMsg = e.getLocalizedMessage();
    log.error(
        "------------GlobalExceptionHandler------处理数据库数据重复异常------>handleDuplicateKeyException:\n{}",
        errMsg);
    if (StringUtils.isNotBlank(errMsg)) {
      String[] errMsgs = errMsg.split("###");
      if (errMsgs.length >= 2) {
        errMsg = errMsgs[1];
        errMsgs = errMsg.split(":");
        errMsg =
            errMsgs[errMsgs.length - 1].replaceAll(" Duplicate entry ", "").replaceAll("\n", "");
      }
    }
    return fail("数据库关键信息重复:" + errMsg);
  }

  /**
   * 处理不支持请求方式
   *
   * @param e ${@link HttpRequestMethodNotSupportedException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:28
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public AnYiResult<String> handleHttpRequestMethodNotSupportedException(
      final HttpRequestMethodNotSupportedException e) {
    e.printStackTrace();
    log.error(
        "------------GlobalExceptionHandler-----处理不支持请求方式------->handleHttpRequestMethodNotSupportedException:\n{}",
        e.getMessage());
    return fail("请求方式不支持:" + e.getMessage());
  }

  /**
   * post请求缺少body参数
   *
   * @param e ${@link HttpMessageNotReadableException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:28
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.OK)
  public AnYiResult<?> handleHttpMessageNotReadableException(
      final HttpMessageNotReadableException e) {
    e.printStackTrace();
    log.error(
        "------------GlobalExceptionHandler------post请求缺少body参数------>handleHttpMessageNotReadableException:\n{}",
        e.getMessage());
    return fail("post请求缺少body参数:" + e.getMessage());
  }

  /**
   * 请求地址不存在
   *
   * @param e ${@link NoHandlerFoundException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:28
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public AnYiResult<String> handleNoSuchElementException(
      final NoHandlerFoundException e, final ServletServerHttpRequest request) {
    e.printStackTrace();
    log.info(
        "------------GlobalExceptionHandler------请求地址不存在------>handleNoSuchElementException:\n{}",
        request.getURI().getPath());
    return fail("请求地址不存在:" + e.getMessage());
  }

  /**
   * 处理数据库唯一性校验失败异常
   *
   * @param e ${@link SQLIntegrityConstraintViolationException}
   * @return AnYiResult ${@link AnYiResult}
   * @author zxh
   * @date 2020-08-27 15:28
   */
  @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public AnYiResult<String> handleSQLIntegrityConstraintViolationException(
      final SQLIntegrityConstraintViolationException e) {
    e.printStackTrace();
    log.error(
        "------------GlobalExceptionHandler------处理数据库唯一性校验失败异常------>handleSQLIntegrityConstraintViolationException:\n{}",
        e.getMessage());
    String errMsg = e.getLocalizedMessage();
    if (StringUtils.isNotBlank(errMsg)) {
      String[] errMsgs = errMsg.split("###");
      if (errMsgs.length >= 2) {
        errMsgs = errMsgs[1].split(":");
        errMsg = errMsgs[errMsgs.length - 1];
        errMsg = errMsg.replaceAll(" Field ", "").replaceAll("\n", "");
      }
    }
    return fail("数据库必填字段没有值:" + errMsg);
  }
}
