plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.13.0"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.apache.commons:commons-text:1.11.0")
  implementation("io.swagger:swagger-annotations:1.6.12")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
  implementation("org.springframework:spring-jms")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.1.1")
  implementation("org.apache.commons:commons-csv:1.10.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.32.0")

  runtimeOnly("com.h2database:h2:2.2.224")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.1")

  testImplementation("org.flywaydb:flyway-core")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
  testImplementation("org.wiremock:wiremock-standalone:3.3.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.19") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.20")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.32.0")
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable"
  )
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
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
