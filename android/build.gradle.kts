plugins {
    id("com.android.application") version "8.13.2" apply false
    kotlin("android") version "1.9.22" apply false
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}
