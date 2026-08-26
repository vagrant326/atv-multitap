plugins {
    alias(libs.plugins.kotlin.jvm)
}

// No Android dependencies here, ever. The press counter and the shipped IME read the same
// keypad, otherwise a measured figure describes a keyboard nobody can install.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

/**
 * KSPC over the query corpus. Lives in the test source set so the runner never reaches the APK.
 *
 * The figure this prints is the floor the rest of the programme is measured against: no
 * dictionary, no model, no cold and warm split, the same number on the first day as on the
 * thousandth. A keyboard that cannot beat it has no argument.
 */
tasks.register<JavaExec>("bench") {
    group = "verification"
    description = "Measures KSPC over bench/queries-v1.tsv"
    mainClass.set("io.github.vagrant326.atvmultitap.core.bench.BenchmarkKt")
    classpath = sourceSets["test"].runtimeClasspath
    workingDir = rootProject.projectDir
    args("--queries", "bench/queries-v1.tsv")
}
