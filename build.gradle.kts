import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML

plugins {
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.spring") version "1.7.0"
    kotlin("plugin.jpa") version "1.7.0"
    kotlin("plugin.allopen") version "1.7.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "10.3.0"
    id("org.openapi.generator") version "6.0.1"
    id("org.owasp.dependencycheck") version "7.1.2"
}

group = "uk.gov.dluhc"
version = "latest"
java.sourceCompatibility = JavaVersion.VERSION_17
ext["snakeyaml.version"] = "1.31"

allOpen {
    annotations("javax.persistence.Entity", "javax.persistence.MappedSuperclass", "javax.persistence.Embedabble")
}

repositories {
    mavenCentral()
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")
apply(plugin = "org.openapi.generator")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

extra["awsSdkVersion"] = "2.17.272"

dependencies {
    // framework
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    // api
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.11")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // webclient
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    // spring security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // jpa/liquibase
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.liquibase:liquibase-core")

    // mysql
    runtimeOnly("mysql:mysql-connector-java")
    runtimeOnly("software.aws.rds:aws-mysql-jdbc:1.1.0")
    runtimeOnly("software.amazon.awssdk:rds")

    // AWS dependencies (that are defined in the BOM "software.amazon.awssdk")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:sqs")

    // AWS signer using SDK V2 library is available at https://mvnrepository.com/artifact/io.github.acm19/aws-request-signing-apache-interceptor/2.1.1
    implementation("io.github.acm19:aws-request-signing-apache-interceptor:2.1.1")

    // tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    testImplementation("net.datafaker:datafaker:1.5.0")

    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
    testImplementation("org.testcontainers:testcontainers:1.17.3")
    testImplementation("org.testcontainers:mysql:1.17.3")
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:${property("awsSdkVersion")}")
    }
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
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "UPPERCASE",
            "useBeanValidation" to "true",
        )
    )
}

tasks.create("Generating RegisterCheckApiModel", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/RegisterCheckerAPIs.yaml")
    packageName.set("uk.gov.dluhc.registercheckerapi")
}

tasks.create("Generating IERApiModel", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/IER-EROP-APIs.yaml")
    packageName.set("uk.gov.dluhc.external.ier")
}

// Add the generated code to the source sets
sourceSets["main"].java {
    this.srcDir("$projectDir/build/generated")
}

// Linting is dependent on GenerateTask
tasks.withType<KtLintCheckTask> {
    dependsOn(tasks.withType<GenerateTask>())
}

// Exclude generated code from linting
ktlint {
    filter {
        exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated/") }
    }
}

/* Configuration for the OWASP dependency check */
dependencyCheck {
    autoUpdate = true
    failOnError = true
    failBuildOnCVSS = 0.toFloat()
    analyzers.assemblyEnabled = false
    analyzers.centralEnabled = true
    format = HTML
    suppressionFiles = listOf("owasp.suppressions.xml")
}
