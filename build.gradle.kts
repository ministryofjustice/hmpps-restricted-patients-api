plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.0.0"
  kotlin("plugin.spring") version "2.1.20"
  kotlin("plugin.jpa") version "2.1.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.apache.commons:commons-text:1.13.0")
  implementation("io.swagger:swagger-annotations:1.6.15")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
  implementation("org.springframework:spring-jms")
  implementation("com.google.code.gson:gson:2.12.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.2")
  implementation("org.apache.commons:commons-csv:1.14.0")
  // Needs to match this version https://github.com/microsoft/ApplicationInsights-Java/blob/3.6.2/dependencyManagement/build.gradle.kts#L16
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.13.1")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.4.2")
  testImplementation("org.flywaydb:flyway-core")
  testImplementation("org.wiremock:wiremock-standalone:3.12.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.25") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.29")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.47.0")
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
    freeCompilerArgs.add("-Xwhen-guards")
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

  task<Test>("testTargetAppScope") {
    include("race-condition-test")
    shouldRunAfter(test)
    useJUnitPlatform()
  }

  build {
    dependsOn(compileKotlin)
    dependsOn("testTargetAppScope")
  }
}
