lazy val root = project
  .in(file("."))
  .settings(
    name := "guapswap",

    version := "1.0.0-beta",

    scalaVersion := "2.12.15",

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.ergoplatform" %% "ergo-appkit" % "4.0.7",
      "com.google.code.gson" % "gson" % "2.8.5",
      "com.monovore" %% "decline-effect" % "2.2.0"
    ),

    libraryDependencySchemes ++= Seq(
    "org.typelevel" %% "cats-kernel" % VersionScheme.Always
    ),

    assembly / assemblyJarName := s"guapswap-${version.value}.jar",
    assembly / assemblyOutputPath := file(s"./guapswap-${version.value}.jar/")

  )
