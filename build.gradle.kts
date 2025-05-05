import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.1.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.gradleup.shadow") version "8.3.6"
}

group = "org.openredstone.velocityutils"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "net.luckperms", name = "api", version = "5.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.40.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.40.1")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.30.1")
    implementation(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
    implementation(group = "co.aikar", name = "acf-velocity", version = "0.5.1-SNAPSHOT")
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "2.13.0")
    implementation(group = "org.javacord", name = "javacord", version = "3.8.0")
    kapt(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.shadowJar {
    relocate("co.aikar.commands", "velocityutils.acf")
    relocate("co.aikar.locales", "velocityutils.locales")
    dependencies {
        exclude(
            dependency(
                "net.luckperms:api:.*"
            )
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}