// Root project
lazy val root = (project in file("."))
  .settings(
    name := "guapswap",
    homepage := Some(url("https://guapswap.org")),
    licenses := Seq("GPL-3.0" -> url("https://spdx.org/licenses/GPL-3.0-or-later.html")),
    description := "GuapSwap TUI the Ergo miner.",
    versionScheme := Some("semver-spec"),
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyOutputPath := file(s"./${name.value}-${version.value}.jar/"),
    commonSettings
  )

// Define common settings applicable to all projects
lazy val commonSettings = Seq(
  scalaVersion := "2.13.0",
  organization := "org.guapswap",
  resolvers ++= Seq(
    "Sonatype Releases".at("https://s01.oss.sonatype.org/content/repositories/releases"),
    "Sonatype Snapshots".at("https://s01.oss.sonatype.org/content/repositories/snapshots")
  ),
  libraryDependencies ++= testing ++ scala_tui
)

// Dependencies
lazy val testing: List[ModuleID] = List(
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17"
)

lazy val scala_tui: List[ModuleID] = List(
  "com.olvind.tui" %% "tui" % "0.0.7"
)