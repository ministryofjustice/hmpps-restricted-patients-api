plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.1"
  kotlin("plugin.spring") version "1.5.30"
  kotlin("plugin.jpa") version "1.5.30"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("io.springfox:springfox-boot-starter:3.0.0")
  implementation("org.springframework:spring-jms")
  implementation("com.google.code.gson:gson:2.8.8")
  implementation("org.awaitility:awaitility-kotlin:4.1.1")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")
  implementation("com.amazonaws:aws-java-sdk-sns:1.12.96")
  implementation("org.springframework.cloud:spring-cloud-aws-messaging:2.2.6.RELEASE")

  runtimeOnly("com.h2database:h2:2.0.206")
  runtimeOnly("org.flywaydb:flyway-core:8.4.1")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.flywaydb:flyway-core:8.0.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable"
  )
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
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
