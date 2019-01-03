/*
 * Copyright 2012 the original author or authors.
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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory


/**
 * ZIP URL installer properties.
 *
 * @see <a href="http://cargo.codehaus.org/Installer">Installer documentation</a>
 */
class ZipUrlInstaller {

    private Property<String> installUrlSupplier

    @Input
    @Optional
    Property<String> installUrl

    @InputFiles
    @Optional
    ConfigurableFileCollection installConfiguration

    @OutputDirectory
    DirectoryProperty downloadDir

    @OutputDirectory
    DirectoryProperty extractDir

    ZipUrlInstaller(ZipUrlInstaller other) {
        installUrl.set(other.installUrl)
        installConfiguration.setFrom(other.installConfiguration)
        installUrlSupplier.set(other.installUrlSupplier)
        downloadDir.set(other.downloadDir)
        extractDir.set(other.extractDir)
    }

    ZipUrlInstaller(ObjectFactory objectFactory, ProjectLayout layout) {
        extractDir = objectFactory.directoryProperty()
        downloadDir = objectFactory.directoryProperty()
        installUrlSupplier = objectFactory.property(String)
        installUrl = objectFactory.property(String)
        installConfiguration = layout.configurableFiles()
    }

    void setDownloadDir(File file) {
        downloadDir.set(file)
    }

    void setExtractDir(File file) {
        extractDir.set(file)
    }

    @Internal
    Provider<String> getConfiguredInstallUrl() {
        installUrlSupplier
    }

    @Internal
    boolean isValid() {
        configuredInstallUrl.present && downloadDir.present && extractDir.present
    }

    void setInstallUrl(String installUrl) {
        this.installUrl.set(installUrl)
        this.installConfiguration.setFrom() // clear
        installUrlSupplier.set(installUrl) // point to installUrl
    }

    void setInstallConfiguration(FileCollection configuration) {
        this.installUrl.set(null)
        this.installConfiguration.setFrom(configuration)
        installUrlSupplier.set(installConfiguration.singleFile.toURI().toURL().toString())
    }

}
