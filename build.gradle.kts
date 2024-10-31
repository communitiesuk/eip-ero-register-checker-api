import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.lang.ProcessBuilder.Redirect

plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.9.24"
    kotlin("kapt") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    kotlin("plugin.allopen") version "1.9.24"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.0.0"
    id("org.openapi.generator") version "7.0.1"
    id("org.owasp.dependencycheck") version "8.2.1"
}

group = "uk.gov.dluhc"
version = "latest"
java.sourceCompatibility = JavaVersion.VERSION_17

extra["awsSdkVersion"] = "2.26.20"
extra["springCloudAwsVersion"] = "3.1.1"

allOpen {
    annotations("jakarta.persistence.Entity", "jakarta.persistence.MappedSuperclass", "jakarta.persistence.Embedabble")
}

val awsProfile = System.getenv("AWS_PROFILE_ARG") ?: "--profile code-artifact"
val codeArtifactToken = "aws codeartifact get-authorization-token --domain erop-artifacts --domain-owner 063998039290 --query authorizationToken --output text $awsProfile".runCommand()

repositories {
    mavenCentral()
    maven {
        url = uri("https://erop-artifacts-063998039290.d.codeartifact.eu-west-2.amazonaws.com/maven/api-repo/")
        credentials {
            username = "aws"
            password = codeArtifactToken
        }
    }
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")
apply(plugin = "org.openapi.generator")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

dependencies {
    // framework
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // internal libs
    implementation("uk.gov.dluhc:logging-library:3.0.3")
    implementation("uk.gov.dluhc:messaging-support-library:2.2.0")
    implementation("uk.gov.dluhc:email-client:1.0.0")

    // api
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.7")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Logging
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.3")

    // spring security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // jpa/liquibase
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.liquibase:liquibase-core")

    // mysql
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("software.aws.rds:aws-mysql-jdbc:1.1.10")
    runtimeOnly("software.amazon.awssdk:rds")

    // AWS dependencies (that are defined in the BOM "software.amazon.awssdk")
    implementation("software.amazon.awssdk:sts")
    // email
    implementation("software.amazon.awssdk:ses")

    // messaging
    implementation("org.springframework:spring-messaging")
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

    // AWS signer using SDK V2 library is available at https://mvnrepository.com/artifact/io.github.acm19/aws-request-signing-apache-interceptor/2.1.1
    implementation("io.github.acm19:aws-request-signing-apache-interceptor:2.3.1")
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Scheduling
    implementation("net.javacrumbs.shedlock:shedlock-spring:4.43.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:4.43.0")

    // tests
    testImplementation("software.amazon.awssdk:sqs") // required to send messages to a queue, which we only need to do in test at the moment
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testImplementation("net.datafaker:datafaker:1.7.0")

    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:mysql:1.19.8")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.withType<GenerateTask>())
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    dependsOn(tasks.withType<GenerateTask>())
    useJUnitPlatform()
}

tasks.withType<GenerateTask> {
    enabled = false
    validateSpec.set(true)
    outputDir.set("$projectDir/build/generated")
    generatorName.set("kotlin-spring")
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "invokers" to "false",
            "models" to "",
        )
    )
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "enumPropertyNaming" to "UPPERCASE",
            "useBeanValidation" to "true",
            "useSpringBoot3" to "true",
        )
    )
}

tasks.create("api-generate RegisterCheckApi model", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/RegisterCheckerAPIs.yaml")
    packageName.set("uk.gov.dluhc.registercheckerapi")
    configOptions.put("documentationProvider", "none")
}

tasks.create("api-generate IERApi model", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/external/ier/reference/IER-EROP-APIs.yaml")
    packageName.set("uk.gov.dluhc.external.ier")
}

tasks.create("api-generate rca-sqs-messaging model", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/sqs/rca-sqs-messaging.yaml")
    packageName.set("uk.gov.dluhc.registercheckerapi.messaging")
}

// Add the generated code to the source sets
sourceSets["main"].java {
    this.srcDir("$projectDir/build/generated")
}

// Linting is dependent on GenerateTask
tasks.withType<KtLintCheckTask> {
    dependsOn(tasks.withType<GenerateTask>())
}

tasks.withType<BootBuildImage> {
    environment.set(mapOf("BP_HEALTH_CHECKER_ENABLED" to "true"))
    buildpacks.set(
        listOf(
            "urn:cnb:builder:paketo-buildpacks/java",
            "gcr.io/paketo-buildpacks/health-checker",
        )
    )
}

// Exclude generated code from linting
ktlint {
    filter {
        exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated/") }
    }
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
    correctErrorTypes = true
}

fun String.runCommand(): String {
    val parts = this.split("\\s".toRegex())
    val process = ProcessBuilder(*parts.toTypedArray())
        .redirectOutput(Redirect.PIPE)
        .start()
    process.waitFor()
    return process.inputStream.bufferedReader().readText().trim()
}

/* Configuration for the OWASP dependency check */
dependencyCheck {
    autoUpdate = true
    failOnError = true
    failBuildOnCVSS = 0.toFloat()
    analyzers.assemblyEnabled = false
    analyzers.centralEnabled = true
    format = HTML.name
    suppressionFiles = listOf("owasp.suppressions.xml")
}
