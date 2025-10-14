import java.util.*

plugins {
    `java-library`
    `maven-publish`
    id("com.gorylenko.gradle-git-properties") version "2.5.3"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("jacoco")
    id("publish-conventions")
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://repo.eternalcode.pl/snapshots")
    maven("https://repo.panda-lang.org/releases")
    maven("https://repo.maven.apache.org/maven2/")
}


dependencies {
    fun shadowApi(dep: Provider<MinimalExternalModuleDependency>) {
        shadow(dep)
        api(dep)
    }
    shadowApi(libs.io.github.snwcreations.jkook)
    shadowApi(libs.com.github.snwcreations.terminalconsoleappender)
    shadowApi(libs.uk.org.lidalia.sysout.over.slf4j)
    shadowApi(libs.org.apache.logging.log4j.log4j.core)
    shadowApi(libs.org.apache.logging.log4j.log4j.slf4j.impl)
    shadowApi(libs.org.fusesource.jansi.jansi)
    shadowApi(libs.org.jline.jline.terminal.jansi)
    shadowApi(libs.net.sf.jopt.simple.jopt.simple)
    shadowApi(libs.com.squareup.okhttp3.okhttp)
    shadowApi(libs.net.kyori.event.api)
    shadowApi(libs.net.kyori.event.method)
    shadowApi(libs.net.freeutils.jlhttp)
    // GSON 已移除 - 项目已完全迁移到 Jackson (v0.52.0+)
    shadow("com.fasterxml.jackson.core:jackson-core:2.17.2"); api("com.fasterxml.jackson.core:jackson-core:2.17.2")
    shadow("com.fasterxml.jackson.core:jackson-databind:2.17.2"); api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    shadow("com.fasterxml.jackson.core:jackson-annotations:2.17.2"); api("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    shadow("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2"); api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    shadowApi(libs.com.github.ben.manes.caffeine.caffeine)
    shadowApi(libs.net.fabricmc.sponge.mixin)
    shadowApi(libs.dev.rollczi.litecommands.framework)
    shadowApi(libs.net.bytebuddy.byte.buddy.agent)
    compileOnly(libs.org.jetbrains.annotations)

    // Test Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.27.2")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")

    // Test runtime dependencies
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}
group = "io.github.snwcreations"
version = "0.32.2"
description = "KookBC"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

gitProperties {
    keys = listOf(
        "git.commit.id",
        "git.commit.id.abbrev",
        "git.branch",
        "git.commit.time"
    )
    gitPropertiesName = "kookbc_git_data.properties"
}

val skipShade = properties["skipShade"] == "true"
tasks.shadowJar {
    enabled = !skipShade
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = ""
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer())
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/services/cpw.*",
    )
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("*.json") {
        expand(properties + mapOf("jkookVersion" to libs.versions.io.github.snwcreations.jkook.get()))
    }
}

tasks.jar {
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/services/cpw.*",
    )
    manifest {
        attributes(
            mapOf(
                "Build-Time" to Date(),
                "Specification-Title" to "JKook",
                "Specification-Version" to libs.io.github.snwcreations.jkook.get().version,
                "Specification-Vendor" to "SNWCreations",
                "Implementation-Title" to "KookBC",
                "Implementation-Version" to version.toString(),
                "Implementation-Vendor" to "SnWCreations",
                "Main-Class" to "snw.kookbc.LaunchMain"
            )
        )
    }
}

// Test Configuration
tasks.test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    testLogging {
        events("passed", "skipped", "failed", "standard_out", "standard_error")
        showStandardStreams = false
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            includes = listOf("snw.kookbc.impl.*")
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// JMH 配置
jmh {
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    jvmArgs.set(listOf("-Xmx4g", "-Xms2g"))
    resultFormat.set("JSON")
    includeTests.set(false)
    includes.set(listOf("JsonProcessingBenchmark"))
}