import org.gradle.kotlin.dsl.register

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.2"
  kotlin("plugin.spring") version "2.2.20"
  kotlin("plugin.jpa") version "2.2.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.apache.commons:commons-text:1.14.0")
  implementation("io.swagger:swagger-annotations:1.6.16")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
  implementation("org.springframework:spring-jms")
  implementation("com.google.code.gson:gson:2.13.2")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.5.0-beta")
  implementation("org.apache.commons:commons-csv:1.14.1")
  // Needs to match this version https://github.com/microsoft/ApplicationInsights-Java/blob/3.7.4/dependencyManagement/build.gradle.kts#L16
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.18.1")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.7.0")
  testImplementation("org.flywaydb:flyway-core")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.34") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.38")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.52.0")
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    freeCompilerArgs.addAll("-Xjvm-default=all", "-Xwhen-guards", "-Xannotation-default-target=param-property")
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
  test {
    useJUnitPlatform {
      excludeTags("race-condition-test")
    }
  }

  register<Test>("testTargetAppScope") {
    include("race-condition-test")
    shouldRunAfter(test)
    useJUnitPlatform()
  }

  build {
    dependsOn(compileKotlin)
    dependsOn("testTargetAppScope")
  }
}
