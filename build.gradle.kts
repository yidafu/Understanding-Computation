plugins {
    kotlin("jvm") version "2.0.0"
}
group = "dev.yidafu.computation"
version = "1.0-SNAPSHOT"

repositories {
    maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation( "io.kotest:kotest-runner-junit5:5.9.0")
    testImplementation( "io.kotest:kotest-property:5.9.0")
    testImplementation ("io.kotest:kotest-assertions-core:5.9.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}