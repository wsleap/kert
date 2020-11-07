package build

import com.google.gradle.osdetector.OsDetector
import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom

object Consts {
  private val osDetector = OsDetector()
  
  val os = osDetector.os
  val arch = osDetector.arch
  val exeSuffix = if(os == "windows") ".exe" else ""

  val pom = Action<MavenPom> {
    name.set("grpc-kt")
    description.set("GRPC stub & compiler for Kotlin Coroutine")
    url.set("https://github.com/xiaodongw/grpc-kt")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("xiaodongw")
        name.set("Xiaodong Wang")
        email.set("xiaodongw79@gmail.com")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/xiaodongw/grpc-kt.git")
      developerConnection.set("scm:git:ssh://github.com/xiaodongw/grpc-kt.git")
      url.set("https://github.com/xiaodongw/grpc-kt.git")
    }
  }
}