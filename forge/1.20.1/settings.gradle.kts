pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.parchmentmc.org/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven("https://maven.minecraftforge.net/")
        maven("https://libraries.minecraft.net/")
        mavenCentral()
    }
}

rootProject.name = "CustomTamingFramework"
