import java.util.*

plugins {
    id("java")
    id("maven-publish")
}

group = "yuv.pink"
version = "1.3"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val properties = Properties().apply {
    load(rootProject.file("local.properties").reader())
}

publishing {
    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/olebeck/pkgextract")
            credentials {
                username = (properties["gpr.user"] ?: System.getenv("GITHUB_USERNAME"))?.toString()
                password = (properties["gpr.key"] ?: System.getenv("GITHUB_TOKEN"))?.toString()
            }
        }
        maven {
            name = "silica.codes"
            url = uri("https://silica.codes/api/packages/olebeck/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${properties["silica.token"]}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = "pkgextract"
                description = "playstation pkg file library"
                licenses {
                    license {
                        name = "The Apache License 2.0"
                        url = "https://opensource.org/licenses/Apache-2.0"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/olebeck/pkgextract.git"
                    developerConnection = "scm:git:git@github.com:olebeck/pkgextract.git"
                    url = "https://github.com/olebeck/pkgextract"
                }
            }
        }
    }
}