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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import java.time.Duration

/**
 * Defines Cargo extension.
 */
class CargoPluginExtension {
    final Property<String> containerId
    final Property<Integer> port
    final Property<Duration> timeout
    final ListProperty<Deployable> deployables
    private ObjectFactory objectFactory

    CargoPluginExtension(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        containerId = objectFactory.property(String)
        port = objectFactory.property(Integer)
        port.set(8080)
        timeout = objectFactory.property(Duration)
        deployables = objectFactory.listProperty(Deployable)
    }

    def cargo(Closure closure) {
        closure.delegate = this
        closure()
    }


    def deployable(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        def deployableClosureDelegate = new DeployableClosureDelegate(project)
        closure.delegate = deployableClosureDelegate
        deployables << deployableClosureDelegate.deployable
        closure()
    }

    private static class DeployableClosureDelegate {

        @Delegate
        final Deployable deployable = new Deployable()
        private final Project project

        DeployableClosureDelegate(Project project) {
            this.project = project
        }

        void setFile(Object file) {
            deployable.files = project.files(file)
        }
    }

}
