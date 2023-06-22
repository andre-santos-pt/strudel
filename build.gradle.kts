import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
}

group = "pt.iscte"
version = "0.8"

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.javaparser:javaparser-symbol-solver-core:3.25.1")
    testApi("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testApi("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation(kotlin("test"))
    testApi(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks {
    register<Jar>("fatJar") {
        group = "distribution"
        archiveClassifier.set("with-dependencies")
        destinationDirectory.set(File("$buildDir/dist"))
        dependsOn.addAll(
            listOf(
                "compileKotlin",
                "processResources"
            )
        )

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}