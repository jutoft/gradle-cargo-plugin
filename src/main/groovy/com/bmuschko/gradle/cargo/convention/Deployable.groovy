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
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal

/**
 * Defines Deployable convention.
 */
class Deployable implements Serializable {
    @Internal
    RegularFileProperty regularFile

    @InputFiles
    ConfigurableFileCollection configuration

    @Input
    Property<String> context

    @javax.inject.Inject
    Deployable(ObjectFactory objectFactory, ProjectLayout projectLayout) {
        regularFile = objectFactory.fileProperty()
        context = objectFactory.property(String)
        configuration = projectLayout.configurableFiles()
        regularFile.convention(configuration.singleFile)
    }

    void setRegularFile(Provider<RegularFile> file) {
        this.regularFile.convention(file)
    }

    @Internal
    File getFile() {
        if(configuration.empty)
            return regularFile.get().asFile
        else
            configuration.singleFile
    }
}
