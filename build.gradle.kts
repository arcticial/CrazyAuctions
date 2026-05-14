plugins {
    id("modrinth-plugin")
    id("hangar-plugin")
    `java-plugin`
}

val branch = "main"
val hash = "0000000"
val commit = "Manual Build"
val releaseType = rootProject.ext["release_type"].toString()
val color = rootProject.property("${releaseType.lowercase()}_color").toString()
val isRelease = releaseType.equals("release", ignoreCase = true)

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier = ""

        val subJars = subprojects
            .filter { it.name != "common" && it.name != "api" }
            .mapNotNull { sub ->
                sub.tasks.jar.get().archiveFile
                    .takeIf { it.isPresent }
                    ?.let { zipTree(it.get().asFile) }
            }

        dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })

        from(subJars) {
            exclude("META-INF/MANIFEST.MF")
        }

        doFirst {
            subJars.forEach { tree ->
                tree.matching { include("META-INF/MANIFEST.MF") }.files.forEach {
                    manifest.from(it)
                }
            }
        }
    }
}
