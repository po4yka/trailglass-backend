import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    }
}

plugins {
    application
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

repositories {
    mavenCentral()
}

val ktorVersion = "3.0.0"
val koinVersion = "3.5.3"
val exposedVersion = "0.53.0"
val hikariVersion = "5.1.0"
val coroutinesVersion = "1.8.1"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive:$ktorVersion")

    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    implementation("de.mkammerer:argon2-jvm:2.11")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.5")
    implementation("io.micrometer:micrometer-core:1.12.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

application {
    mainClass.set("com.trailglass.backend.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

tasks.test {
    useJUnitPlatform()
}
