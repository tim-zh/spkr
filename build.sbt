name := "spkr"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.reactivemongo" %% "reactivemongo" % "0.11.9"
)

routesGenerator := InjectedRoutesGenerator

fork in run := true