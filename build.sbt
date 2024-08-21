import BuildSettings._

lazy val `test-util` = UtilsBuild.`test-util`
lazy val `test-util2` = UtilsBuild.`test-util2`

name := "sbt-release-example"
ThisBuild / scalaVersion := buildScalaVersion

