plugins {
    kotlin("multiplatform") version "1.7.20"

    // for some reason we need these, else get error
    // "Cannot add task 'commonizeNativeDistribution' as a task with that name already exists."
    kotlin("plugin.serialization") version "1.7.20"
    id("tech.formatter-kt.formatter") version "0.7.9"
}

repositories {
    google()
    mavenCentral()
}

// create build/libs/native/main/hacllib-cinterop-libhacl.klib
kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val arch = System.getProperty("os.arch")
    val nativeTarget =
        when {
            hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported.")
        }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libhacl by
                creating {
                    defFile(project.file("nativeInterop/libhacl.def"))
                    packageName("hacl")
                    compilerOpts("-Ilibhacl/include")
                    includeDirs.allHeaders("${System.getProperty("user.dir")}/libhacl/include")
                }
            }
        }
    }
}

// create build/libhacl.a
tasks.register("libhaclBuild") {
    doLast {
        exec {
            workingDir(".")
            // -p flag will ignore errors if the directory already exists
            commandLine("mkdir", "-p", "build")
        }
        exec {
            workingDir("build")
            commandLine("cmake", "-DCMAKE_BUILD_TYPE=Release", "..")
        }
        exec {
            workingDir("build")
            commandLine("make")
        }
    }
}

// hack to make sure that we've compiled the library prior to running cinterop on it
tasks["cinteropLibhaclNative"].dependsOn("libhaclBuild")