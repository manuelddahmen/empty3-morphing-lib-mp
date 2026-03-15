import java.util.Properties
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.io.File
import java.io.FileInputStream

plugins {
    id("application") // enabling the plugin here
    id("java")
    id("java-library")
    id("maven-publish")
    id("org.jreleaser") version("1.23.0")
    id("signing")
    //id("org.jetbrains.kotlin.jvm") version("2.3.10")
    kotlin("jvm")
}
val properties = Properties()
try {
    // Other configuration here
    val propertiesFile = File("../private.properties/gradle.properties")
    if (propertiesFile.exists()) {
        properties.load(FileInputStream(propertiesFile))
    }
} catch (ex : Exception) {
    ex.printStackTrace()
}

signing {
    try {
        val signingKey = properties.get("signing.keyId")
        val signingPassword = properties.get("signing.password")
        useGpgCmd()
    } catch ( e:Exception) {
        // Ignorer les erreurs de signature si les clés ne sont pas disponibles
        logger.warn("Signature configuration is not complete, skipping signing: ${e.message}")
    }
}

val JAVA_VERSION = 25
val versionName = "2026.3.14-25"
java {
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(JAVA_VERSION)
}
tasks.configureEach {
    if (name.startsWith("generate") && (name.endsWith("Builders") || name.endsWith("Accessors"))) {
        doNotTrackState("Cannot access output property")
    }
}

tasks.register("testMorphing", JavaExec::class.java) {
    classpath = sourceSets.getByName("main").runtimeClasspath
    mainClass = "TestMorphingKt"
}


dependencies {
    implementation("one.empty3:empty3-library-mp:2026.3.14-21")
    implementation("one.empty3.libs:partial-desktop:0.0.39-17")
    implementation("one.empty3.libs:commons-mp:0.0.17-17")
    implementation(kotlin("stdlib-jdk8"))
}

val vArtifactId = "empty3-library-mp-morphing"
val vGroupId = "one.empty3"
configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = vGroupId
            artifactId = vArtifactId
            version = versionName
            from(components["java"])

            // The pom can be enriched as usual
            pom {
                name.set(versionName)
                description.set("Morphing library extension")
                url.set("https://github.com/manuelddahmen/empty3-library-mp")
                inceptionYear.set("2025")
                licenses {
                    license {
                        name.set("Apache version 2")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                scm {

                    url.set("https://github.com/manuelddahmen/empty3-library-mp")
                    connection.set("scm:git:https://github.com/manuelddahmen/empty3-library-mp.git")
                    developerConnection.set("scm:git:https://github.com/manuelddahmen/empty3-library-mp.git")

                }
                developers {
                    developer {
                        email.set("manuel.dahmen@gmx.com")
                        name.set("Manuel Daniel Dahmen")

                        url.set("https://empty3.one")
                        id.set("manuelddahmen")
                    }
                }

            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}




jreleaser {
    signing {

        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        // mode.set(org.jreleaser.model.Signing.Mode.FILE) // mode might be different in Gradle DSL
        publicKey.set(properties.getProperty("signing.publicKeyPath"))
        passphrase.set(properties.getProperty("signing.passphrase"))
        secretKey.set(properties.getProperty("signing.secretKey"))
        release.github.token = properties.getProperty("release.github.token")

    }
    project {
        name.set(project.name)
        java {
            artifactId.set(vArtifactId)
            java {
                artifactId.set(vArtifactId)
                groupId.set(vGroupId)
                version.set(versionName)
            }
            version.set(versionName)
            // license.set("Apache-2.0")
            authors.set(listOf("Manuel D DAHMEN"))
            description.set("Online morphing api")
            copyright.set("Author : Manuel D. Dahmen, License: Apache-2")
            icon {
                // Publicly available URL. PNG format is preferred.
                //
                url.set("https://empty3.one/favicon.ico")
                // Marks this icon as the primary one.
                // Only a single icon may be set as primary.
                //
                primary.set(true)
                // Image width in pixels.
                //
                width.set(47)
                // Image height in pixels.
                //
                height.set(47)
            }
        }

        deploy {
            maven {
                // Enable or disable all configured deployers.
                // Supported values are [`NEVER`, `ALWAYS`, `RELEASE`, `SNAPSHOT`].
                // Defaults to `ALWAYS`.
                //
                active.set(org.jreleaser.model.Active.ALWAYS)
                pomchecker {
                    // Defines the tool version to use.
                    //
                    version.set("1.11.0")

                    // Fail the release if pomchecker outputs a warning.
                    // Defaults to `true`.
                    //
                    failOnWarning.set(false)

                    // Fail the release if pomchecker outputs an error.
                    // Defaults to `true`.
                    //
                    failOnError.set(false)
                }
                mavenCentral {
                    create("app") {
                        active.set(org.jreleaser.model.Active.ALWAYS)
                        url.set("https://central.sonatype.com/api/v1/publisher")
                        // gitRootSearch.set(false)
                        username.set(properties.getProperty("JRELEASER_MAVENCENTRAL_USERNAME"))
                        password.set(properties.getProperty("JRELEASER_MAVENCENTRAL_PASSWORD"))
                        authorization.set(org.jreleaser.model.Http.Authorization.BASIC)
                        // Password for login into the MAVENCENTRAL service.
                        //
                        // List of directories where staged artifacts can be found.
                        stagingRepository("build/staging-deploy")

                        // Defines the connection timeout in seconds.
                        // Defaults to `20`.
                        //
                        connectTimeout.set(20)

                        // Defines the read timeout in seconds.
                        // Defaults to `60`.
                        //
                        readTimeout.set(60)


                        // Registered publication namespace.
                        // Defaults to `${project.java.groupId}`.
                        //
                        // namespace.set("one.empty3")

                        // Deployment identifier used for publication.
                        //
                        deploymentId.set("27d4cb92-0141-4a3f-b2e1-38c801bfafb0")

                        // Time to wait between state transition checks, in seconds.
                        // Defaults to `10`.
                        //
                        retryDelay.set(10)

                        // Maximum number of attempts to verify state transition.
                        // Defaults to `60`.
                        //
                        maxRetries.set(120)
                    }
                }
            }
        }
        upload {
            // Enable or disable all configured uploaders.
            // Supported values are [`NEVER`, `ALWAYS`, `RELEASE`, `SNAPSHOT`].
            // Defaults to `ALWAYS`.
            //
            // active = "ALWAYS"
        }
    }
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(25)
}