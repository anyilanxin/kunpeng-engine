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

final class SebGeneratorConst {

    static final DEFAULT_SBE_VERSION = '1.35.6'

    static final PLUGIN_GROUP = 'SBE Generator'
    static final EXTENSION_NAME = 'sbeGenerator'
    static final CONFIGURATION_NAME = 'sbeGenerator'

    static final VALIDATE_TASK = 'sbeValidate'
    static final VALIDATE_TASK_DESCRIPTION = 'Validates message declaration schemas'

    static final GENERATE_JAVA_TASK = 'sbeGenerateJavaCodecs'
    static final GENERATE_JAVA_TASK_DESCRIPTION = 'Generates Java SBE codecs'

    static final COMPILE_JAVA_TASK = 'sbeCompileJavaCodecs'
    static final COMPILE_JAVA_DESCRIPTION = 'Compiles Java SBE codecs'

    static final PACK_JAVA_TASK = 'sbeJavaArchive'
    static final PACK_JAVA_TASK_DESCRIPTION = 'Creates Java jar with SBE codecs'

    static final GENERATE_CPP_TASK = 'sbeGenerateCppCodecs'
    static final GENERATE_CPP_TASK_DESCRIPTION = 'Generates C++ SBE codecs'

    static final CMAKE_CPP_TASK = 'sbeCppCmakeScripts'
    static final CMAKE_CPP_TASK_DESCRIPTION = 'Prepares C++ CMake scripts for SBE library'

    static final PACK_CPP_TASK = 'sbeCppArchive'

    static final TMP_DIR = 'build/tmp'

    static final DEFAULT_SRC_DIR = 'src/main/resources/xml'

    static final DEFAULT_JAVA_CODECS_DIR = 'build/generated/src/main/java'
    static final DEFAULT_JAVA_CLASSES_DIR = 'build/generated/classes'

    static final DEFAULT_CPP_CODECS_DIR = 'build/generated/src/main/cpp'
    static final DEFAULT_CPP_CMAKE_PROJECT_DIR = 'build/generated/cmake-project'

    static final DEFAULT_ARCHIVES_DIR = 'build/archives'

    static final DEFAULT_JAVA_SOURCE_COMPATIBILITY = '1.8'
    static final DEFAULT_JAVA_TARGET_COMPATIBILITY = '1.8'

    static final DEFAULT_SHOULD_VALIDATE = false

    static final PROJECT_PROPERTY_SBE_VERSION = 'sbeVersion'

    private SebGeneratorConst() {
    }
}
