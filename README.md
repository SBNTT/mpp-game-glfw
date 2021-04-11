![CD](https://github.com/SBNTT/mpp-game-glfw/workflows/CD/badge.svg)
![GLFW_RELEASES_WATCHER](https://github.com/SBNTT/mpp-game-glfw/workflows/GLFW_RELEASES_WATCHER/badge.svg)
![VULKAN_RELEASES_WATCHER](https://github.com/SBNTT/mpp-game-glfw/workflows/VULKAN_RELEASES_WATCHER/badge.svg)

# Kotlin mpp GLFW
## Usage
```kotlin
repositories {
    maven("https://maven.pkg.github.com/SBNTT/mpp-game-glfw") {
        credentials {
            username = "SBNTT-robot"
            password = String(Base64.getDecoder().decode("Z2hwX3VoTENMZ2xBa3dmdmdPRjRSRDBodDl6RFNqUGdCOTBjZnBONw=="))
        }
    }
}

dependencies {
    implementation("me.sbntt.mppgame:glfw:$glfwVersion-vulkan.$vulkanVersion")
}
```

This credentials uses a personaal access token with only the `read:packages` scope ;)
It can be freely used as downloading from GitHub packages requires authentication...

```kotlin
import me.sbntt.mppgame.glfw.*
```