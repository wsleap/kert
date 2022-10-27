package build

import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom

object Consts {
  val pom = Action<MavenPom> {
    name.set("kert")
    description.set("Concise HTTP & GRPC library for Kotlin")
    url.set("https://github.com/wsleap/kert")
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
      connection.set("scm:git:git://github.com/wsleap/kert.git")
      developerConnection.set("scm:git:ssh://github.com/wsleap/kert.git")
      url.set("https://github.com/wsleap/kert.git")
    }
  }
}
