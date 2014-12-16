name := "kct-server"

version := "0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "io.prismic" %% "scala-kit" % "1.2.16",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "bootstrap" % "3.1.1-2",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "requirejs" % "2.1.11-1"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
