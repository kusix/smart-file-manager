plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {

    // AWS SDK v2
    implementation("software.amazon.awssdk:s3:2.20.135")
    implementation("software.amazon.awssdk:dynamodb:2.20.135")
    implementation("software.amazon.awssdk:apache-client:2.20.135")

    // Lambda 支持
    implementation("com.amazonaws:aws-lambda-java-core:1.2.2")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.2")

    // JSON 处理
    implementation("com.google.code.gson:gson:2.10.1")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    // Mockito for mocking
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 定义打包任务
tasks.register<Zip>("packageLambda") {
    archiveBaseName.set("smart-file-manager")
    archiveClassifier.set("lambda")
    destinationDirectory.set(file("build/distributions"))

    // 包含编译后的类和资源文件
    from(sourceSets.main.get().output)

    // 包含所有运行时依赖
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // 排除签名文件以避免冲突
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

    // 设置重复文件处理策略
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE  // 排除重复文件
}

// 将 packageLambda 任务绑定到 build 任务
tasks.named("build") {
    dependsOn("packageLambda")
}

tasks.test {
    useJUnitPlatform()
}