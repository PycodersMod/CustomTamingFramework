import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    eclipse
    idea
    `maven-publish`
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

version = "0.1.5-alpha"
group = "com.pycoder.customtamingframework"

base {
    archivesName.set("custom_taming_framework")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val pycodersRunDir = file(
    providers.gradleProperty("pycodersRuntimeDir")
        .orElse("../../runtime/legacy-import/CustomTamingFramework/run")
        .get()
)
fun decodeArgs(name: String): List<String> = providers.gradleProperty(name).orNull?.takeIf { it.isNotEmpty() }?.split('.')?.map { if (it == "_") "" else String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) } ?: emptyList()
val pycodersGameArgs = decodeArgs("pycodersGameArgsB64")
val pycodersJavaArgs = decodeArgs("pycodersJavaArgsB64")
val pycodersUsername = providers.gradleProperty("pycodersUsername").orElse("Dev").get()

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
            workingDirectory(pycodersRunDir)
            pycodersJavaArgs.forEach { jvmArg(it) }
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods {
                create("custom_taming_framework") {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            workingDirectory(pycodersRunDir)
            args("--username", pycodersUsername)
            pycodersGameArgs.forEach { args(it) }
        }
        create("server") {
            workingDirectory(pycodersRunDir)
            args("--nogui")
            pycodersGameArgs.forEach { args(it) }
        }
        create("gameTestServer")
        create("data") {
            workingDirectory(pycodersRunDir)
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
