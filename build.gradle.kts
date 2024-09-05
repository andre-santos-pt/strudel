import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.8.20"
    id("maven-publish")
}

group = "pt.iscte"
version = "0.9.1"

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.javaparser:javaparser-symbol-solver-core:3.26.1")
    testApi("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testApi("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    exclude("**/temp/**")

}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    //compilerOptions {
    //    freeCompilerArgs.add("-Xjvm-default=all")
    //}
}

tasks {
    register<Jar>("fatJar") {
        group = "distribution"
        archiveClassifier.set("standalone")
        destinationDirectory.set(File("$buildDir/dist"))
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        )
        manifest {
            attributes["Main-Class"] = "pt.iscte.strudel.MainJarKt"
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents) {
            exclude("**/*.RSA","**/*.SF","**/*.DSA")
        }
    }
}


publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/andre-santos-pt/strudel")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        register<MavenPublication>("strudel") {
            from(components["java"])
        }
    }
}