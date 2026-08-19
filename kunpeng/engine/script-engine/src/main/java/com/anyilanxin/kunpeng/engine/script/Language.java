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
package com.anyilanxin.kunpeng.engine.script;

/**
 * @author zxuanhong
 * @date 2025-11-10 11:59
 * @since
 */
public enum Language {
  DEFAULT("QL_EXPRESS", "", "ql express引擎"),
  QL_EXPRESS("QL_EXPRESS", "", "ql express引擎"),
  PYTHON("PYTHON", "python", "python引擎"),
  JAVA_SCRIPT("JAVA_SCRIPT", "Graal.js", "graal js引擎"),
  ;

  private final String language;
  private final String shortName;
  private final String description;

  Language(final String language, final String shortName, final String description) {
    this.language = language;
    this.shortName = shortName;
    this.description = description;
  }

  public static Language getByLanguage(final String language) {
    for (final Language value : Language.values()) {
      if (value.language.equals(language)) {
        return value;
      }
    }
    return DEFAULT;
  }

  public String getLanguage() {
    return language;
  }

  public String getShortName() {
    return shortName;
  }

  public String getDescription() {
    return description;
  }
}
