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
package structpack

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Gradle plugin that registers an {@code autoDeclareProperties} task for projects
 * using {@code @AutoDeclareProperties}.  The task is wired before compilation and
 * before Spotless so generated source is stable before formatting checks run.
 */
class AutoDeclarePropertiesPlugin implements Plugin<Project> {

  @Override
  void apply(final Project project) {
    final def autoDeclareTask =
        project.tasks.register("autoDeclareProperties", AutoDeclarePropertiesTask) { task ->
          task.group = 'build'
          task.description = 'Auto-generates declareProperty chains for @AutoDeclareProperties annotated classes'
        }

    // Only Java projects have source sets; defer wiring until the Java plugin is
    // applied so the convention plugin can be applied in any order.
    project.plugins.withType(JavaPlugin) {
      autoDeclareTask.configure { task ->
        task.sourceFiles =
            project.sourceSets.main.java.files.findAll { it.path.contains('src/main/java') }
      }

      project.tasks.withType(JavaCompile).configureEach { compileTask ->
        compileTask.dependsOn(autoDeclareTask)
      }
    }

    // Spotless may be configured on the root project; ensure generated code is
    // stable before it runs.
    project.rootProject.tasks.matching { it.name == 'spotlessJava' }.configureEach { spotlessTask ->
      spotlessTask.dependsOn(autoDeclareTask)
    }
  }
}
