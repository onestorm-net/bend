rootProject.name = "bend"

listOf("protocol", "client-api", "client-netty", "server-netty").forEach {
    include(":bend-$it")
    project(":bend-$it").projectDir = file(it)
}
