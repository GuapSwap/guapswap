lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "guapswap",
      scalaVersion := "2.12.0"
    )),
    name := "guapswap"
  )

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.ergoplatform" %% "ergo-appkit" % "3.2.1"
)
