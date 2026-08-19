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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection

import static SebGeneratorConst.*

final class SebGeneratorUtil {

    private SebGeneratorUtil() {
    }

    static ConfigurableFileTree tree(Project project, Src src) {
        def m = [dir: src.dir, includes: ['*.xml']]
        if (src.includes) {
            m.put('includes', src.includes)
        }
        if (src.excludes) {
            m.put('excludes', src.excludes)
        }
        return project.fileTree(m)
    }

    static void prepareRepositories(Project project) {
        project.repositories {
            mavenCentral()
        }
        project.repositories.addAll(project.buildscript.repositories)
    }

    static Configuration addSbeDependency(Project project,
                                          SbeGeneratorPluginExtension extension, String configName) {
        def logger = project.logger

        def sbeVersion = project.getProperties()
                .computeIfAbsent(PROJECT_PROPERTY_SBE_VERSION,
                { k -> DEFAULT_SBE_VERSION }
                )

        logger.info("Using SBE version: $sbeVersion")

        def sbeAllArtifact = "uk.co.real-logic:sbe-all:$sbeVersion"
        def sbeToolArtifact = "uk.co.real-logic:sbe-tool:$sbeVersion:sources"

        def config = project.getConfigurations().maybeCreate(configName)
        project.getDependencies().add(config.getName(), sbeAllArtifact)
        project.getDependencies().add(config.getName(), sbeToolArtifact)
        return config;
    }

    static FileCollection sbeClasspath(Project project,
                                       SbeGeneratorPluginExtension extension, Configuration config) {
        try {
            return project.files(config.getFiles())
        } catch (final Exception ex) {
            def sbeVersion = project.getProperties().get(PROJECT_PROPERTY_SBE_VERSION)
            throw new IllegalArgumentException(
                    "SBE '$sbeVersion' not found", ex)
        }
    }
}
