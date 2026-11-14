plugins {
    `kotlin-dsl`
}

// compiled by whichever JDK runs Gradle but for Java 21 so nothing extra has to be installed
// (e.g. the Docker image only has Java 25)
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

gradlePlugin {
    plugins {
        register("tailwind") {
            id = "org.limelightcreatives.tailwind"
            implementationClass = "org.limelightcreatives.gradle.tailwind.TailwindPlugin"
            displayName = "Tailwind CSS"
            description = "Builds Tailwind CSS with the standalone CLI and integrates with IntelliJ IDEA"
        }
    }
}
