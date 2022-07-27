repositories {
  // Fix for https://github.com/JetBrains/gradle-intellij-plugin/issues/520
  mavenCentral()
}

plugins {
  id("org.jetbrains.intellij") version "1.6.0"
  kotlin("jvm") version "1.6.20"
}

intellij {
  pluginName.set("inin")
  version.set("LATEST-EAP-SNAPSHOT")
  type.set("IC")
  updateSinceUntilBuild.set(false)
}

tasks {
  publishPlugin {
    token.set(System.getenv("PLUGINS_JB_TOKEN"))
  }
}
