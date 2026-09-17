dependencies {
    compileOnly(libs.netty.all)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.jspecify)

    testImplementation(libs.netty.all)
    testImplementation(libs.assertj.core)
}
