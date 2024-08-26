# Publishing sbt Modules Like a Pro

`sbt` (Scala Build Tool) is a powerful build automation tool designed for Scala and Java projects. It handles tasks such as dependency management, compilation, testing, and deployment.

The `sbt-release` plugin enhances `sbt` by automating the release process. It manages versioning, tagging, and artifact publishing, ensuring consistent, reproducible releases.

## The Goal
We aim to automate the release of `sbt` packages without manually editing version files or typing complex `sbt` commands. Upon merging to the master branch, the version should automatically increment, and the package should be published to the target repository.

### Release Workflow
1. **Create a Feature Branch:** Make necessary changes on a new branch.
2. **CI Validation:** Ensure the build and tests pass through Continuous Integration (CI).
3. **Merge to Master:** Once CI passes, merge the branch into master.
4. **Version Bump:** Use GitHub Actions to automatically bump the version (via `sbt-release`).
5. **Publish Package:** Publish the package to the desired repository (e.g., Nexus) using GitHub Actions.
6. **Sync Master:** Merge the updated version back into the master branch.

## Repository Structure
To publish multiple packages from the same repository, structure the modules so that each has its own directory and version file.

## Project Settings
Here’s how you can define project settings in your `sbt` build:

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
      publishTo := Some("Nexus" at "https://some.host.server.com/repository/maven-releases/"),
      releaseVersionFile := file(versionFile),
      scalaVersion := buildScalaVersion,
      releaseNextVersion := { ver =>
        Version(ver).map(_.bump(releaseVersionBump.value).string).getOrElse(versionFormatError(ver))
      },
      releaseTagName := s"$module-v${releaseNextVersion.value.apply(currentVersion)}",
      versionScheme := Some("early-semver"),
      releaseProcess := finalReleaseSteps
    )
  )
}

def getCurrentVersion(versionFile: String): String = {
  val file = new File(versionFile)
  val source = Source.fromFile(file)
  val lines = try source.mkString finally source.close()
  val version = lines.split("=")(1).trim.replace("\"", "")
  version
}
```

### Explanation:
1. **Version File Location:** Specifies the location and name of the version file for each module.
2. **Current Version Retrieval:** Fetches the current version using `getCurrentVersion`.
3. **Version Bumping:** Defines how the `sbt-release` plugin should bump the version.

```scala
releaseNextVersion := {
  ver => Version(ver).map(_.bump(releaseVersionBump.value).string).getOrElse(versionFormatError(ver))
}
```

## Release Steps
The `sbt-release` plugin defines specific steps for setting and bumping the next version:

```scala
private val finalReleaseSteps = Seq[ReleaseStep](
  checkSnapshotDependencies,  // Check for snapshot dependencies
  inquireVersions,            // Ask for the next version number
  runClean,                   // Clean the project (sbt clean)
  runTest,                    // Run tests (sbt test)
  setNextVersion,             // Bump the module version
  commitNextVersion,          // Commit the new version
  tagRelease,                 // Tag the new release in Git
  publishArtifacts,           // Publish artifacts (e.g., to Nexus)
  pushChanges                 // Push changes to the repository
)
```

## Automate with GitHub Actions
Finally, integrate the automation using a GitHub Actions workflow:
1. Checkout the master branch after a merge.
2. Identify modified modules.
3. Execute the `sbt-release` plugin.
4. Comment on the PR with the released version details.

For a practical example, check out the [Release Workflow](https://github.com/hagay3/sbt-release-example/blob/master/.github/workflows/release.yaml).

## Example Source Code
Explore the full source code [here](https://github.com/hagay3/sbt-release-example).
