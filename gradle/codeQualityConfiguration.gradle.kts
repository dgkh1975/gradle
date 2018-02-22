/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.GroovyBasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.quality.CodeNarcExtension
import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.tasks.SourceSet
import org.gradle.plugin.devel.tasks.ValidateTaskProperties
import java.io.File

// This script can not be moved to buildSrc/main/kotlin as it is also used by the buildSrc build.

// As this script is also accessed from the buildSrc project,
// we can't use rootProject for the path as both builds share the same config directory.
val codeQualityConfigDir = File(buildscript.sourceFile!!.parentFile.parentFile, "config")

configureCheckstyle(codeQualityConfigDir)
configureCodenarc(codeQualityConfigDir)

fun Project.configureCheckstyle(codeQualityConfigDir: File) {
    apply {
        plugin("checkstyle")
    }

    configure<CheckstyleExtension> {
        configDir = File(codeQualityConfigDir, "checkstyle")

        plugins.withType<GroovyBasePlugin> {
            java.sourceSets.all {
                tasks.create<Checkstyle>(getTaskName("checkstyle", "groovy")) {
                    configFile = File(configDir, "checkstyle-groovy.xml")
                    source(allGroovy)
                    classpath = compileClasspath
                    reports.xml.destination = File(reportsDir, "${this@all.name}-groovy.xml")
                }
            }
        }
    }
}

fun Project.configureCodenarc(codeQualityConfigDir: File) {
    apply {
        plugin("codenarc")
    }

    dependencies {
        "codenarc"("org.codenarc:CodeNarc:1.0")
        components {
            withModule("org.codenarc:CodeNarc") {
                allVariants {
                    withDependencies {
                        removeAll { it.group == "org.codehaus.groovy" }
                        add("org.codehaus.groovy:groovy-all") {
                            version { prefer("2.4.12") }
                            because("We use groovy-all everywhere")
                        }
                    }
                }
            }
        }
    }

    configure<CodeNarcExtension> {
        configFile = File(codeQualityConfigDir, "codenarc.xml")
    }

    tasks.withType<CodeNarc> {
        reports.xml.isEnabled = true
    }
}

private val Project.java
    get() = the<JavaPluginConvention>()

private val SourceSet.allGroovy: SourceDirectorySet
    get() = withConvention(GroovySourceSet::class) { allGroovy }
