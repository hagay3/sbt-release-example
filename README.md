# Publishing sbt modules like a champ

* `sbt` (Scala Build Tool) is a build automation tool specifically designed for Scala and Java projects, providing features like dependency management, compilation, testing, and deployment.

* `sbt-release` plugin - a tool for the Scala Build Tool (SBT) that automates the release process of a project by managing versioning, tagging, and publishing artifacts, ensuring consistent and reproducible releases.


## The mission
We want to release sbt packages automatically without modifying version files and without typing awkward sbt commands.
Upon merge to master - automatically bump the version and publish the package to a desired repository.

### Release flow
1. Create a new branch with the desired modified files
2. Go trough the CI flow to make sure build and tests finish successfully
3. Merge the branch to master
4. Github Actions: Automatically bump the package version (with sbt-release)
5. Github Actions: Publish the package to a desired repo (e.g Nexus)
6. Github Actions: Merge the bumped version to master branch

## Repo structure
In order to publish multiple packages from the same repository we can have the modules structured and each module will have it's own directory and version.


## Define project settings
```scala
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
          publishTo := {
            Some("Nexus" at "https://some.host.server.com/repository/maven-releases/")
            ,
          },
          releaseVersionFile := file(versionFile),
          scalaVersion := buildScalaVersion,
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
        ))
  }

  def getCurrentVersion(versionFile: String): String = {
    val file = new File(versionFile)
    val source = Source.fromFile(file)
    val lines = try source.mkString finally source.close()
    val version = lines.split("=")(1).trim
      .replace("\"", "")
    version
  }
```
With the above sbt project settings we can define:
1. The version file location and name for each module
2. Fetching current version of the module with ``getCurrentVersion``
3. Telling sbt-release plugin how to bump the version with:
```scala
   releaseNextVersion := {
      ver => Version(ver).map(_.bump(releaseVersionBump.value).string).getOrElse(versionFormatError(ver))
   }
```

## Define the release steps
sbt-release plugin works with specific steps to set and bump the next sbt version. 
```scala
  private val finalReleaseSteps = Seq[ReleaseStep](
    checkSnapshotDependencies, // : ReleaseStep
    inquireVersions, // : ReleaseStep
    runClean, // : sbt clean
    runTest, // : sbt test
    setNextVersion, // : bump the module version (according to releaseNextVersion)
    commitNextVersion, // : git commit
    tagRelease, // : git tag (the new version)
    publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up (e.g: publish artifacts to nexus)
    pushChanges // : git push
  )
```

## Wrap the automation with Github Actions
Finally, wrapping the automation with Github Actions workflow:
1. Checkout master branch upon merge
2. List all modified modules
3. Execute sbt-release plugin command
4. Add comment to PR with released version

https://github.com/hagay3/sbt-release-example/blob/master/.github/workflows/release.yaml



## Example Source code
https://github.com/hagay3/sbt-release-example