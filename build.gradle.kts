import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    idea
    id("org.springframework.boot") version "4.1.2" // todo(fut): version catalog
}

group = "org.limelightcreatives"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
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

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

tasks.test {
    useJUnitPlatform()

    jvmArgumentProviders.add(JavaAgent(files(mockitoAgentClasspath)))
    // mockito on the bootstrap classpath stops class data sharing covering anything but jdk classes
    // and the jvm warns about it so turn sharing off (same speed, no warning)
    jvmArgs("-Xshare:off")
}

/**
 * Adds the given [jar] as a java agent using the `-javaagent:<jar>` JVM argument
 *
 * @property jar the JAR to use as a Java agent
 */
class JavaAgent(@get:Classpath val jar: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-javaagent:${jar.singleFile.absolutePath}")
}
