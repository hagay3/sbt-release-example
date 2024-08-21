import BuildSettings.*
import sbt.*

object UtilsBuild {

  // This module is for testing nexus release automation
  lazy val `test-util` = baseModule("test-util", "utils")

  // This module is for testing nexus release automation
  lazy val `test-util2` =
    baseModule("test-util2", "utils")
      .dependsOn(`test-util`)

}
