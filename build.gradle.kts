@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*

repositories {
    mavenCentral()
}

plugins {
    kotlin("multiplatform")
}

group   = "me.sbntt.mpp.glfw"
version = "0.1.0"

val glfwVersion   = "3.3.2"
val vulkanVersion = "1.2.165"

val nativeLibsDir = buildDir.resolve("nativeLibs")
val downloadsDir  = buildDir.resolve("tmp")

val vulkanDir    = nativeLibsDir.resolve("vulkan-$vulkanVersion")
val glfwMacosDir = nativeLibsDir.resolve("glfw-$glfwVersion-macos")
val glfwMingwDir = nativeLibsDir.resolve("glfw-$glfwVersion-mingw")
val glfwLinuxDir = nativeLibsDir.resolve("glfw-$glfwVersion-linux")

tasks {
    val setupVulkan by registering {
        downloadNativeLibFromGithubAsset(
            url = "https://github.com/KhronosGroup/Vulkan-Headers/archive",
            asset = "v$vulkanVersion.zip",
            dest = vulkanDir
        )
    }

    val setupMacosGlfw by registering {
        downloadNativeLibFromGithubAsset(
            url = "https://github.com/glfw/glfw/releases/download/$glfwVersion",
            asset = "glfw-$glfwVersion.bin.MACOS.zip",
            dest = glfwMacosDir
        )
    }

    val setupMingwGlfw by registering {
        downloadNativeLibFromGithubAsset(
            url = "https://github.com/glfw/glfw/releases/download/$glfwVersion",
            asset = "glfw-$glfwVersion.bin.WIN64.zip",
            dest = glfwMingwDir
        )
    }

    val setupLinuxGlfw by registering {
        downloadNativeLibFromGithubAsset(
            url = "https://github.com/glfw/glfw/releases/download/$glfwVersion",
            asset = "glfw-$glfwVersion.zip",
            dest = glfwLinuxDir
        ) {
            println("Building GLFW...")
            listOf(
                "cmake .",
                "make",
                "mkdir lib-linux",
                "mv src/libglfw3.a lib-linux"
            ).forEach { println(it.runCommand(glfwLinuxDir)) }
        }
    }

    val buildFromMacos by registering {
        tasksFiltering("compile", "", false, "ios", "macos").forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }

    val publishFromMacos by registering {
        tasksFiltering("publish", "GitHubPackagesRepository", false, "ios", "macos").forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }

    val buildFromLinux by registering {
        (tasksFiltering("compile", "", false, "android", "linux")).forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }

    val publishFromLinux by registering {
        tasksFiltering("publish", "GitHubPackagesRepository", false, "android", "linux").forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }

    val buildFromWindows by registering {
        tasksFiltering("compile", "", false, "mingw").forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }

    val publishFromWindows by registering {
        tasksFiltering("publish", "GitHubPackagesRepository", false, "mingw").forEach {
            dependsOn(this@tasks.getByName(it))
        }
    }
}

kotlin {
    macosX64()
    mingwX64()
    linuxX64()

    targets.withType<KotlinNativeTarget>().forEach {
        it.compilations.named("main") {
            val (setupGlfwTask, glfwIncludeDir, staticLibraryPath) = when(konanTarget) {
                    KonanTarget.MACOS_X64 -> listOf(
                        tasks.named("setupMacosGlfw"),
                        glfwMacosDir.resolve("include"),
                        "$glfwMacosDir/lib-macos/libglfw3.a"
                    )
                    KonanTarget.MINGW_X64 -> listOf(
                        tasks.named("setupMingwGlfw"),
                        glfwMingwDir.resolve("include"),
                        "$glfwMingwDir/lib-mingw-w64/libglfw3.a"
                    )
                    else -> listOf(
                        tasks.named("setupLinuxGlfw"),
                        glfwLinuxDir.resolve("include"),
                        "$glfwLinuxDir/lib-linux/libglfw3.a"
                    )
                }

                setupGlfwTask as TaskProvider<Task>
                glfwIncludeDir as File
                staticLibraryPath as String

                cinterops.create("glfw") {
                    tasks.named(interopProcessingTaskName) {
                        dependsOn(tasks.named("setupVulkan"))
                        dependsOn(setupGlfwTask)
                    }

                    includeDirs(glfwIncludeDir, vulkanDir.resolve("include"))
                }

                kotlinOptions {
                    freeCompilerArgs = listOf(
                        "-include-binary", staticLibraryPath
                    )
                }
        }
    }
}

fun downloadNativeLibFromGithubAsset(url: String, asset: String, dest: File, runAfter: (() -> Unit)? = null) {
    if (dest.exists()) return

    nativeLibsDir.mkdirs()
    if (!downloadsDir.exists()) downloadsDir.mkdirs()

    println("Downloading $asset ...")
    val archive = downloadsDir.resolve(asset)
    download("$url/$asset", archive)

    println("Expanding $asset ...")
    copy {
        from(zipTree(archive)) {
            includeEmptyDirs = false
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
            }
        }
        into(dest)
    }

    delete(archive)
    runAfter?.invoke()
}

fun download(url : String, dest: File) {
    ant.invokeMethod("get", mapOf("src" to url, "dest" to dest))
}

fun tasksFiltering(prefix: String, suffix: String, test: Boolean, vararg platforms: String) = tasks.names
        .asSequence()
        .filter { it.startsWith(prefix, ignoreCase = true) }
        .filter { it.endsWith(suffix, ignoreCase = true) }
        .filter { it.endsWith("test", ignoreCase = true) == test }
        .filter { it.contains("test", ignoreCase = true) == test }
        .filter { task -> platforms.any { task.contains(it, ignoreCase = true) } }
        .toMutableList()

fun String.runCommand(workingDir: File = file("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText().trim()
}