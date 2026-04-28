plugins {
    java
    jacoco
    checkstyle
    id("com.github.node-gradle.node") version "7.1.0"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

node {
    version.set("20.11.1")
    download.set(true)
    nodeProjectDir.set(file("frontend"))
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.5")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.microsoft.playwright:playwright:1.45.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform {
        if (!project.hasProperty("includePlaywright")) {
            excludeTags("playwright")
        }
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}

val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "build"))
    inputs.dir("frontend/src")
    inputs.file("frontend/package.json")
    inputs.file("frontend/vite.config.ts")
    inputs.file("frontend/tsconfig.json")
    inputs.file("frontend/index.html")
    outputs.dir("frontend/dist")
}

tasks.processResources {
    dependsOn(buildFrontend)
    from("frontend/dist") {
        into("static")
    }
}

val verifyFrontendBundle by tasks.registering {
    dependsOn(tasks.processResources)
    doLast {
        val indexHtml = layout.buildDirectory.file("resources/main/static/index.html").get().asFile
        require(indexHtml.exists()) {
            "Frontend bundle missing: ${indexHtml.absolutePath}"
        }
    }
}

tasks.check {
    dependsOn(verifyFrontendBundle)
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

checkstyle {
    toolVersion = "10.17.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    configProperties =
        mapOf(
            "suppressionFile" to rootProject.file("config/checkstyle/suppressions.xml").absolutePath,
        )
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.register("lint") {
    group = "verification"
    description = "Run all static checks (spotlessCheck + checkstyle)."
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
}
