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
package sbe

import javax.inject.Inject

import static SebGeneratorConst.*

import javax.naming.spi.ObjectFactory

import org.gradle.api.Action
import org.gradle.api.Project

import groovy.transform.ToString

@ToString(includeFields = true, includeNames = true)
class Src {

    String dir
    Iterable includes
    Iterable excludes

    Src(String dir, Iterable includes = [], Iterable excludes = []) {
        this.dir = dir
        this.includes = includes
        this.excludes = excludes
    }
}

class SbeGeneratorPluginExtension {

    def src = new Src(DEFAULT_SRC_DIR)

    def javaCodecsDir = DEFAULT_JAVA_CODECS_DIR
    def javaClassesDir = DEFAULT_JAVA_CLASSES_DIR

    def javaSourceCompatibility = DEFAULT_JAVA_SOURCE_COMPATIBILITY
    def javaTargetCompatibility = DEFAULT_JAVA_TARGET_COMPATIBILITY

    def cppCodecsDir = DEFAULT_CPP_CODECS_DIR
    def cppCmakeDir = DEFAULT_CPP_CMAKE_PROJECT_DIR

    def archivesDir = DEFAULT_ARCHIVES_DIR

    def javaOptions = [:]
    def cppOptions = [:]

    def shouldValidate = DEFAULT_SHOULD_VALIDATE

    SbeGeneratorPluginExtension(Project project) {
    }

    @Inject
    SbeGeneratorPluginExtension(ObjectFactory objectFactory) {
        src = objectFactory.newInstance(Src)
    }

    void src(Action<? super Src> action) {
        action.execute(src)
    }
}
