import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("io.gitlab.arturbosch.detekt") version "1.22.0"
	kotlin("jvm") version "1.7.21"
	jacoco
	application
}

group = "c64"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

val kotlinVersion: String by extra
val kotlinLoggingVersion: String by extra
val kotlinCoroutinesVersion: String by extra
val logstashLogbackVersion: String by extra
val kotlinMockitoVersion: String by extra
val slf4jVersion: String by extra
val logbackVersion: String by extra

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

	implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
	implementation("org.slf4j:slf4j-api:$slf4jVersion")
	implementation("ch.qos.logback:logback-classic:$logbackVersion")

	implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackVersion")

	testImplementation("com.nhaarman:mockito-kotlin:$kotlinMockitoVersion")
}

application {
	mainClass.set("c64.emulation.ui.EmulatorUIKt")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict", "-java-parameters")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
}


