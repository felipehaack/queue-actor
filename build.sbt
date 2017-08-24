name := "queue"

version := "1.0"

scalaVersion in ThisBuild := "2.12.2"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.16" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
