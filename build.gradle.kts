import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sonatypeCentralPortalPublisher)
    `maven-publish`
}

group = "app.simplecloud"
version = "1.0.4"

repositories {
    mavenCentral()
    maven("https://buf.build/gen/maven")
}

dependencies {
    testImplementation(rootProject.libs.kotlinTest)
    implementation(rootProject.libs.kotlinJvm)
    api(rootProject.libs.bundles.proto)
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

tasks.test {
    useJUnitPlatform()
}

centralPortal {
    name = project.name

    username = project.findProperty("sonatypeUsername") as? String
    password = project.findProperty("sonatypePassword") as? String

    pom {
        name.set("SimpleCloud PubSub")
        description.set("PubSub library for simplecloud")
        url.set("https://github.com/theSimpleCloud/simplecloud-pubsub")

        developers {
            developer {
                id.set("fllipeis")
                email.set("p.eistrach@gmail.com")
            }
        }
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/theSimpleCloud/simplecloud-pubsub.git")
            connection.set("git:git@github.com:theSimpleCloud/simplecloud-pubsub.git")
        }
    }
}

signing {
    sign(publishing.publications)
    useGpgCmd()
}
