pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Pode manter, mas não será mais usado para o kshoji
        maven { url = uri("https://github.com/kshoji/USB-MIDI-Driver/raw/master/MIDIDriver/snapshots") }
        maven { url = uri("https://github.com/kshoji/BLE-MIDI-for-Android/raw/master/library/repository") }
        maven { url = uri("https://github.com/kshoji/javax.sound.midi-for-Android/raw/develop/javax.sound.midi/repository") }
    }
}

rootProject.name = "android-midi-synth"
include(":app")
