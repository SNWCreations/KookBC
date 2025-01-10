import java.util.*

plugins {
    `java-library`
    `maven-publish`
    id("com.gorylenko.gradle-git-properties") version "2.4.2"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("publish-conventions")
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
    shadowApi(libs.com.github.snwcreations.jkook)
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
    shadowApi(libs.com.google.code.gson.gson)
    shadowApi(libs.com.github.ben.manes.caffeine.caffeine)
    shadowApi(libs.net.fabricmc.sponge.mixin)
    shadowApi(libs.dev.rollczi.litecommands.framework)
    shadowApi(libs.net.bytebuddy.byte.buddy.agent)
    compileOnly(libs.org.jetbrains.annotations)
}
group = "io.github.snwcreations"
version = "0.32.2"
description = "KookBC"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

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
    archiveClassifier = ""
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer())
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/services/cpw.*",
    )
}

tasks.processResources {
    filesMatching("*.json") {
        expand(properties + mapOf("jkookVersion" to libs.versions.com.github.snwcreations.jkook.get()))
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
                "Specification-Version" to libs.com.github.snwcreations.jkook.get().version,
                "Specification-Vendor" to "SNWCreations",
                "Implementation-Title" to "KookBC",
                "Implementation-Version" to version.toString(),
                "Implementation-Vendor" to "SnWCreations",
                "Main-Class" to "snw.kookbc.LaunchMain"
            )
        )
    }
}