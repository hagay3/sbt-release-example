import sbt.Keys.*
import sbt.librarymanagement.Configurations.Test
import sbt.{Resolvers as _, *}
import sbtrelease.ReleasePlugin.autoImport.*
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
import sbtrelease.{Version, versionFormatError}
import scala.io.Source

object BuildSettings {
  val buildScalaVersion = "2.13.10"
  val rootProjectName = "sbt-release-example"
  val compileWithTest = "compile->compile; test->test"

  // Inspired by https://tpolecat.github.io/2017/04/25/scalac-flags.html
  // See comments about the flags there and in the official documentation
  // https://docs.scala-lang.org/overviews/compiler-options/index.html
  val scalacBuildOptions = Seq(
    "-encoding", "UTF-8",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-deprecation",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-Xcheckinit",
    "-Xfuture",
    "-Xlint:adapted-args",
    "-Wextra-implicit",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:nullary-unit",
    "-Wvalue-discard",
    "-Xlint:unused",
    "-Wmacros:after", // so that we don't get warnings on unused imports around macros
    "-P:silencer:checkUnused",
    "-P:silencer:pathFilters=src_managed",
  )


  /**
    * Get Current Version for specific module
    *
    * @param versionFile - the file that contains the version
    */
  def getCurrentVersion(versionFile: String): String = {
    val file = new File(versionFile)
    val source = Source.fromFile(file)
    val lines = try source.mkString finally source.close()
    val version = lines.split("=")(1).trim
      .replace("\"", "")
    version
  }


  def baseModule(module: String, subFolder: String): Project = {
    val versionFile = s"./modules/$subFolder/$module/$module.version.sbt"
    val currentVersion = getCurrentVersion(versionFile)
    Project(
      id = module,
      base = file(s"./modules/$subFolder/$module")
    ).settings(
        Seq(
          organization := s"$rootProjectName.$subFolder.",
          name := module,
          credentials in Global += Credentials(file("./nexus/credentials")),
          publishTo := {
            Some("Nexus" at "https://some.host.server.com/repository/maven-releases/")
            ,
          },
          releaseVersionFile := file(versionFile),
          scalaVersion := buildScalaVersion,
          releaseUseGlobalVersion := false,
          releaseIgnoreUntrackedFiles := true,
          packageDoc / publishArtifact := false,
          releaseNextVersion := {
            ver =>
              Version(ver).map(_.bump(releaseVersionBump.value).string).getOrElse(versionFormatError(ver))
          },
          releaseTagName := s"$module-v${releaseNextVersion.value.apply(currentVersion)}",
          versionScheme := Some("early-semver"),
          // sbt-release plugin steps
          releaseProcess := {
            finalReleaseSteps
          },
        ),
        asciiGraphWidth := 10000)
      .configs(IntegrationTest)
  }



  private val finalReleaseSteps = Seq[ReleaseStep](
    checkSnapshotDependencies, // : ReleaseStep
    inquireVersions, // : ReleaseStep
    runClean, // : ReleaseStep
    runTest, // : ReleaseStep
    setNextVersion, // : ReleaseStep
    commitNextVersion, // : ReleaseStep
    tagRelease, // : ReleaseStep
    publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
    pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
  )

}
