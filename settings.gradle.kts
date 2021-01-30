val name: String by settings
rootProject.name = name

pluginManagement {
    plugins {
        kotlin("multiplatform") version "1.4.21"
    }
}
