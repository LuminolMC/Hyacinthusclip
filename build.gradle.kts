plugins {
    java
    application
    `maven-publish`
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

val mainClass = "moe.luminolmc.hyacinthusclip.Main"

tasks.jar {
    val java6Jar = project(":java6").tasks.named("jar")
    val java21Jar = project(":java21").tasks.named("shadowJar")
    dependsOn(java6Jar, java21Jar)

    from(zipTree(java6Jar.map { it.outputs.files.singleFile }))
    from(zipTree(java21Jar.map { it.outputs.files.singleFile }))

    manifest {
        attributes(
            "Main-Class" to mainClass
        )
    }

    from(file("license.txt")) {
        into("META-INF/license")
        rename { "paperclip-LICENSE.txt" }
    }
    from(file("license.txt")) {
        into("META-INF/license")
        rename { "hyacinthusclip-LICENSE.txt" }
    }
    rename { name ->
        if (name.endsWith("-LICENSE.txt")) {
            "META-INF/license/$name"
        } else {
            name
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    val java6Sources = project(":java6").tasks.named("sourcesJar")
    val java21Sources = project(":java21").tasks.named("sourcesJar")
    dependsOn(java6Sources, java21Sources)

    from(zipTree(java6Sources.map { it.outputs.files.singleFile }))
    from(zipTree(java21Sources.map { it.outputs.files.singleFile }))

    archiveClassifier.set("sources")
}

val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(sourcesJar)
            withoutBuildIdentifier()

            pom {
                val repoPath = "LuminolMC/Hyacinthusclip"
                val repoUrl = "https://github.com/$repoPath"

                name.set("Hyacinthusclip")
                description.set(project.description)
                url.set(repoUrl)
                packaging = "jar"

                licenses {
                    license {
                        name.set("MIT")
                        url.set("$repoUrl/blob/main/license.txt")
                        distribution.set("repo")
                    }
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("$repoUrl/issues")
                }

                developers {
                    developer {
                        id.set("DemonWav")
                        name.set("Kyle Wood")
                        email.set("demonwav@gmail.com")
                        url.set("https://github.com/DemonWav")
                    }
                    developer {
                        id.set("MrHua269")
                        name.set("MrHua269")
                        email.set("mrhua269@gmail.com")
                        url.set("https://github.com/MrHua269")
                    }
                }

                scm {
                    url.set(repoUrl)
                    connection.set("scm:git:$repoUrl.git")
                    developerConnection.set("scm:git:git@github.com:$repoPath.git")
                }
            }
        }

        repositories {
            maven("https://repo.menthamc.org/repository/maven-snapshots/") {
                name = "MenthaMC"
                credentials(PasswordCredentials::class) {
                    username = System.getenv("PRIVATE_MAVEN_REPO_USERNAME")
                    password = System.getenv("PRIVATE_MAVEN_REPO_PASSWORD")
                }
            }
        }
    }
}

tasks.register("printVersion") {
    doFirst {
        println(version)
    }
}
