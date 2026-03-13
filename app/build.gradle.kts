plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.kotlinPluginSpring)
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.springdocOpenapi)
    implementation(libs.playwright)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
