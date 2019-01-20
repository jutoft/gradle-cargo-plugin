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
package com.bmuschko.gradle.cargo.tasks.local

import com.bmuschko.gradle.cargo.DeployableType
import com.bmuschko.gradle.cargo.DeployableTypeFactory
import com.bmuschko.gradle.cargo.convention.BinFile
import com.bmuschko.gradle.cargo.convention.ConfigFile
import com.bmuschko.gradle.cargo.convention.Deployable
import com.bmuschko.gradle.cargo.convention.ZipUrlInstaller
import com.bmuschko.gradle.cargo.tasks.AbstractCargoContainerTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import java.time.Duration

/**
 * Deploys WAR to local container.
 */
class LocalCargoContainerTask extends AbstractCargoContainerTask {
    /**
     * Level representing the quantity of information we wish to log.
     */
    @Input
    @Optional
    final Property<String> logLevel = project.objects.property(String)

    /**
     * JVM args to be used when starting/stopping containers.
     */
    @Input
    @Optional
    final Property<String> jvmArgs = project.objects.property(String)

    /**
     * The container's installation home directory.
     */
    @Internal
    @Optional
    final DirectoryProperty homeDir = project.objects.directoryProperty()

    /**
     * The Cargo configuration home directory.
     */
    @Internal
    @Optional
    final DirectoryProperty configHomeDir = project.objects.directoryProperty()

    /**
     * The Cargo configuration type.
     */
    @Internal
    @Optional
    final Property<String> configType = project.objects.property(String)

    /**
     * The path to a file where container logs are saved.
     */
    @OutputFile
    @Optional
    final RegularFileProperty outputFile = project.objects.fileProperty()

    /**
     * The path to a file where Cargo logs are saved.
     */
    @OutputFile
    @Optional
    final RegularFileProperty logFile = project.objects.fileProperty()

    /**
     * The port to use when communicating with this server.
     */
    @Input
    @Optional
    final Property<Integer> rmiPort = project.objects.property(Integer)

    /**
     * Timeout after which the container start/stop is deemed failed.
     */
    @Input
    @Optional
    final Property<Duration> timeout = project.objects.property(Duration)

    /**
     * The system properties passed-on the container.
     */
    @Input
    MapProperty<String, Object> systemProperties = project.objects.mapProperty(String, Object)

    /**
     * The list of configuration files.
     */
    @Input
    ListProperty<ConfigFile> configFiles = project.objects.listProperty(ConfigFile)

    /**
     * The list of binary files.
     */
    @Input
    ListProperty<BinFile> files = project.objects.listProperty(BinFile)

    /**
     * Configurable ZIP URL installer instance for automatically downloading a container.
     */
    @Nested
    ZipUrlInstaller zipUrlInstaller = new ZipUrlInstaller(project.objects, project.layout)

    /**
     * Additional libraries for your application's classpath that are not exposed to the container.
     */
    @InputFiles
    @Optional
    ConfigurableFileCollection sharedClasspath = project.layout.configurableFiles()

    /**
     * Additional libraries added to the container's classpath.
     */
    @InputFiles
    @Optional
    ConfigurableFileCollection extraClasspath = project.layout.configurableFiles()


    @Override
    void validateConfiguration() {
        super.validateConfiguration()

        if (getDeployables().isPresent()) {
            getDeployables().get().each { deployable ->
                if (deployable.file.present && !deployable.file.get().asFile.exists()) {
                    throw new InvalidUserDataException("Deployable "
                            + (deployable.file == null ? "null" : deployable.file.canonicalPath)
                            + " does not exist")
                }
            }

            logger.info("Deployable artifacts = ${getDeployables().get().collect { it.file.get().asFile.canonicalPath }}")
        }

        if (getConfigFiles().isPresent()) {
            getConfigFiles().get().each { configFile ->
                if(!configFile.file || !configFile.file.exists()) {
                    throw new InvalidUserDataException("Config file "
                    + (configFile.file == null ? "null" : configFile.file.canonicalPath)
                    + " does not exist")
                }
            }

            logger.info("Config files = ${getConfigFiles().get().collect { it.file.canonicalPath + " -> " + it.toDir }}")
        }

        if (getFiles().isPresent()) {
            getFiles().get().each { binFile ->
                if (!binFile.file || !binFile.file.exists()) {
                    throw new InvalidUserDataException("Binary Property<File> "
                    + (binFile.file == null ? "null" : binFile.file.canonicalPath)
                    + " does not exist")
                }
            }
            logger.info("Binary files = ${getFiles().get().collect { it.file.canonicalPath + " -> " + it.toDir }}")
        }

        if (getConfigType().isPresent()) {
            if (!['standalone', 'existing'].contains(getConfigType().get())) {
                throw new InvalidUserDataException("Configuration type " + getConfigType().get() + " is not supported, use 'standalone' or 'existing'")
            }
        }
    }

    @Override
    void runAction() {
        logger.info "Starting action '${getAction()}' for local container '${getContainerId()}'"

        ant.taskdef(resource: CARGO_TASKS, classpath: getClasspath().asPath)
        ant.cargo(getCargoAttributes()) {
            ant.configuration(getConfigurationAttributes()) {
                property(name: AbstractCargoContainerTask.CARGO_SERVLET_PORT, value: getPort().get().toString())

                if (getJvmArgs().present) {
                    ant.property(name: 'cargo.jvmargs', value: getJvmArgs().get())
                }

                if (getLogLevel().present) {
                    ant.property(name: 'cargo.logging', value: getLogLevel().get())
                }

                if (getRmiPort().present) {
                    ant.property(name: 'cargo.rmi.port', value: getRmiPort().get())
                }

                setContainerSpecificProperties()

                getDeployables().get().each { Deployable deployable ->
                    DeployableType deployableType = DeployableTypeFactory.instance.getType(deployable.file)

                    if(deployable.context.present) {
                        ant.deployable(type: deployableType.type, file: deployable.file.asFile) {
                            ant.property(name: AbstractCargoContainerTask.CARGO_CONTEXT, value: deployable.context.get())
                        }
                    } else {
                        ant.deployable(type: deployableType.type, file: deployable.file.asFile)
                    }
                    }

                getConfigFiles().get().each { configFile ->
                    ant.configfile(file: configFile.file, todir: configFile.toDir)
                }

                getFiles().get().each { binFile ->
                    ant.file(file: binFile.file, todir: binFile.toDir)
                }
            }

            setSystemProperties()

            if(getZipUrlInstaller().isValid()) {
                ant.zipUrlInstaller(installUrl: getZipUrlInstaller().configuredInstallUrl.get(), downloadDir: getZipUrlInstaller().downloadDir.get().asFile,
                        extractDir: getZipUrlInstaller().extractDir.get().asFile)
            }

            if (!getExtraClasspath().empty) {
                ant.extraClasspath() {
                    getExtraClasspath().addToAntBuilder(ant, 'fileset', FileCollection.AntType.FileSet)
                }
            }

            if (!getSharedClasspath().empty) {
                ant.sharedClasspath() {
                    getSharedClasspath().addToAntBuilder(ant, 'fileset', FileCollection.AntType.FileSet)
                }
            }
        }
    }

    @Input
    protected Map<String, String> getConfigurationAttributes() {
        def config = [:]

        if (getConfigHomeDir().present) {
            File homeDir = getConfigHomeDir().get().asFile
            if (!homeDir.exists()) {
                ant.mkdir(homeDir)
            }

            config['home'] = homeDir.absolutePath
        }

        if (getConfigType().present) {
            config['type'] = getConfigType().get()
        }

        return config
    }

    private Map<String, String> getCargoAttributes() {
        def cargoAttributes = ['containerId': getContainerId().get(), 'action': getAction()]

        if (getTimeout().present) {
            cargoAttributes['timeout'] = getTimeout().get()
        }

        if(!getZipUrlInstaller().isValid()) {
            cargoAttributes['home'] = getHomeDir().get().asFile.canonicalPath
        }

        if (getOutputFile().present) {
            cargoAttributes['output'] = getOutputFile().get().asFile
        }

        if (getLogFile().present) {
            cargoAttributes['log'] = getLogFile().get().asFile
        }

        cargoAttributes
    }

    void setSystemProperties() {
        logger.info "System properties = ${getSystemProperties().get()}"

        getSystemProperties().get().each { key, value ->
            ant.sysproperty(key: key, value: value)
        }
    }
}
