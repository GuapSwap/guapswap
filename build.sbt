lazy val root = project
  .in(file("."))
  .settings(
    name := "guapswap",

    version := "0.1.0",

    scalaVersion := "2.12.10",

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.ergoplatform" %% "ergo-appkit" % "4.0.6",
      "com.google.code.gson" % "gson" % "2.8.5"
    ),

    resolvers ++= Seq(
      "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
      "SonaType" at "https://oss.sonatype.org/content/groups/public",
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
    )
  )
