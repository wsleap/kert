package build

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

fun Project.versionCatalog(name: String): VersionCatalog {
  return extensions.getByType<VersionCatalogsExtension>().named(name)
}

fun VersionCatalog.library(name: String) = findLibrary(name).get()
fun VersionCatalog.version(name: String) = findVersion(name).get()
