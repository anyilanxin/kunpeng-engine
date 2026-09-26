/*
 * Copyright (c) 2021-present ZHOUXUANHONG(安一老厨)<anyilanxin@aliyun.com>
 *
 * 本软件 AnYi Cloud EE 为 AnYi Cloud 的商业授权软件。未经过商业授权禁止使用，违者必究。
 *
 * AnYi Cloud EE 为商业授权软件，您在使用过程中，需要注意以下几点：
 *   1.不允许在国家法律法规规定的范围外使用，如出现违法行为作者本人不承担任何责任；
 *   2.软件使用的第三方依赖皆为开源软件，如需要修改第三方依赖请遵循第三方依赖附带的开源协议，因擅自修改第三方依赖所引起的争议，作者不承担任何责任；
 *   3.不得基于AnYi Cloud EE的基础，修改包装而成一个与AnYi Cloud EE、AnYi Zeebe EE、AnYi Standalone EE功能类似的程序，进行销售或发布，参与同类软件产品市场的竞争；
 *   4.不得将软件源码以任何开源方式公布出去；
 *   5.不得对授权进行出租、出售、抵押或发放子许可证；
 *   6.您可以直接使用在自己的网站或软件产品中，也可以集成到您自己的商业网站或软件产品中进行出租或销售；
 *   7.您可以对上述授权软件进行必要的修改和美化，无需公开修改或美化后的源代码；
 *   8.本软件流程部分请遵循camunda开源协议：
 *     https://docs.camunda.org/manual/latest/introduction/third-party-libraries
 *     https://github.com/camunda/camunda-bpm-platform/blob/master/LICENSE
 *   9.除满足上面条款外，在其他商业领域使用不受影响。同时作者为商业授权使用者在使用过程中出现的纠纷提供协助。
 */
package com.anyilanxin.core;

/**
 * 全局公共常量
 *
 * @author zxh
 * @date 2020-07-23 19:57
 * @since 1.0.0
 */
public interface CommonCoreConstant {

  String CONST_PREFIX = "corecommon_";

  /** 返回数据格式 */
  String DATA_FORMAT = "data_format";

  /** http */
  String HTTP = "http://";

  /** https */
  String HTTPS = "https://";

  /** 斜杠 */
  String SLASH = "/";

  /** 冒号 */
  String COLON = ":";

  /** 横杠 */
  String DASH = "-";

  /** 空 */
  String EMPTY = "";

  /** 点 */
  String POINT = ".";

  /** 时区 */
  String TIME_ZONE_GMT8 = "GMT+8";

  /** 测试缓存key */
  String TEST_DEMO_CACHE = "anyi:cache";

  /** 开发环境标记 */
  String DEV = "DEV";

  /** 测试环境标记 */
  String TEST = "TEST";

  /** 正式环境标记 */
  String PRO = "PRO";

  /** 雪花终端id */
  long WORKER_ID = 12L;

  /** 雪花数据中心id */
  long DATACENTER_ID = 12L;

  /** 是否允许灰度调度环境变量key */
  String ENABLE_GRAY_KEY = "spring.cloud.nacos.discovery.metadata.gray-info.enable-gray";

  /** 灰度信息key */
  String GRAY_HEADER_KEY = "x-gray-info";

  /** 线程池前缀 */
  String TASK_EXECUTOR_PREFIX = "taskExecutor-";

  String SYSTEM_PREFIX = "ANYI_LANXIN_";

  /** 跟踪信息 */
  String X_REQUEST_ID = "x-request-id";

  /** 锁过期时间:单位秒 */
  long LOCK_EXPIRES = 30;
}
