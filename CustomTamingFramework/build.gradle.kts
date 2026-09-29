plugins {
    eclipse
    idea
    `maven-publish`
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

version = "0.1.5-alpha"
group = "com.example.customtamingframework"

base {
    archivesName.set("custom_taming_framework")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
    mavenCentral()
}

minecraft {
    mappings("official", "1.20.1")
    copyIdeResources.set(true)
    val ctfUsernameProvider = providers.gradleProperty("ctfUsername").orElse("Dev")

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods {
                create("custom_taming_framework") {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            workingDirectory(file("../../runtime/legacy-import/CustomTamingFramework/run"))
            args("--username", ctfUsernameProvider.get())
        }
        create("server") {
            args("--nogui")
        }
        create("gameTestServer")
        create("data") {
            workingDirectory(project.file("run-data"))
            args(
                "--mod", "custom_taming_framework",
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.4.20")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "custom_taming_framework",
            "Specification-Vendor" to "Pycoder",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Pycoder"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.jar)
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
