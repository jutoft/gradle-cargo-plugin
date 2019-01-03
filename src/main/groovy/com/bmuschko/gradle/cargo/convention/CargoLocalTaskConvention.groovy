/*
 * Copyright 2011 the original author or authors.
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
package com.bmuschko.gradle.cargo.convention

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Defines Cargo local task convention.
 */
class CargoLocalTaskConvention {
    final Property<String> jvmArgs
    final Property<String> logLevel
    final DirectoryProperty homeDir
    final Property<String> configType
    final DirectoryProperty configHomeDir
    final RegularFileProperty outputFile
    final RegularFileProperty logFile
    final Property<Integer> rmiPort
    final ListProperty<ConfigFile> configFiles
    final ListProperty<BinFile> files
    final ZipUrlInstaller zipUrlInstaller
    final MapProperty<String, Object> containerProperties
    final MapProperty<String, Object> systemProperties
    final ConfigurableFileCollection extraClasspath
    final ConfigurableFileCollection sharedClasspath

    CargoLocalTaskConvention(ObjectFactory objectFactory, ProjectLayout layout) {
        jvmArgs = objectFactory.property(String)
        logLevel = objectFactory.property(String)
        homeDir = objectFactory.directoryProperty()
        configType = objectFactory.property(String)
        configHomeDir = objectFactory.directoryProperty()
        outputFile = objectFactory.fileProperty()
        logFile = objectFactory.fileProperty()
        rmiPort = objectFactory.property(Integer)
        configFiles = objectFactory.listProperty(ConfigFile)
        files = objectFactory.listProperty(BinFile)
        extraClasspath = layout.configurableFiles()
        sharedClasspath = layout.configurableFiles()
        containerProperties = objectFactory.mapProperty(String, Object)
        systemProperties = objectFactory.mapProperty(String, Object)
        zipUrlInstaller = new ZipUrlInstaller(objectFactory, layout)
    }

    def installer(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = zipUrlInstaller
        closure()
    }

    def configFile(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        ConfigFile configFile = new ConfigFile()
        closure.delegate = configFile
        configFiles << configFile
        closure()
    }

    def file(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        BinFile file = new BinFile()
        closure.delegate = file
        files << file
        closure()
    }

    def containerProperties(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = new MapPropertyExtension(containerProperties)
        closure()
    }

    def systemProperties(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = new MapPropertyExtension(systemProperties)
        closure()
    }
}
