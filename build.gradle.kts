import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    idea
    id("org.springframework.boot") version "4.1.2" // todo(fut): version catalog
    id("org.limelightcreatives.tailwind")
}

group = "org.limelightcreatives"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tailwind {
    version = providers.gradleProperty("tailwind.version")
}

repositories {
    mavenCentral()
}

// todo(fut): plugin?
val mockitoAgent = configurations.dependencyScope("mockitoAgent")
val mockitoAgentClasspath = configurations.resolvable("mockitoAgentClasspath") {
    extendsFrom(mockitoAgent)
}

dependencies {
    val springBoot = platform(SpringBootPlugin.BOM_COORDINATES)
    implementation(springBoot)
    annotationProcessor(springBoot)
    developmentOnly(springBoot)
    mockitoAgent(springBoot)

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-restclient") // google sheets
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // a real postgres for tests and local runs from maven so nothing needs installing (no docker)
    // the binaries are one jar per platform and only this machine's is downloaded (see below)
    testImplementation("io.zonky.test:embedded-postgres:2.2.2") { exclude(group = "io.zonky.test.postgres") }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

// postgres binaries for the machine running the build: macos (x64/arm64), windows (x64), linux (x64/arm64)
// anywhere else there are none and tests need TEST_DATABASE_URL (see TestPostgres)
val embeddedPostgresBinaries = providers.systemProperty("os.name")
    .zip(providers.systemProperty("os.arch")) { osName, osArch ->
        val os = osName.lowercase().let {
            when {
                "mac" in it -> "darwin"
                "win" in it -> "windows"
                "linux" in it -> "linux"
                else -> null
            }
        }
        val cpu = when (osArch.lowercase()) {
            "amd64", "x86_64" -> "amd64"
            "aarch64", "arm64" -> "arm64v8"
            else -> null
        }
        val published = setOf("darwin-amd64", "darwin-arm64v8", "windows-amd64", "linux-amd64", "linux-arm64v8")
        listOfNotNull("$os-$cpu".takeIf { it in published })
            .map { dependencies.create("io.zonky.test.postgres:embedded-postgres-binaries-$it:18.6.0") }
    }

configurations.testRuntimeOnly {
    dependencies.addAllLater(embeddedPostgresBinaries)
}

sourceSets.main {
    tailwind {
        content.from("src/main/jte", "src/main/resources/static/js", "src/main/java")
    }
}

tasks.test {
    useJUnitPlatform()

    jvmArgumentProviders.add(JavaAgent(files(mockitoAgentClasspath)))
    // mockito on the bootstrap classpath stops class data sharing covering anything but jdk classes
    // and the jvm warns about it so turn sharing off (same speed, no warning)
    jvmArgs("-Xshare:off")
}

// bootRun needs a postgres you run yourself, bootTestRun starts one (see LocalDashboardApp)
tasks.bootRun {
    systemProperty("spring.profiles.active", "dev")
}

/**
 * Adds the given [jar] as a java agent using the `-javaagent:<jar>` JVM argument
 *
 * @property jar the JAR to use as a Java agent
 */
class JavaAgent(@get:Classpath val jar: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-javaagent:${jar.singleFile.absolutePath}")
}
