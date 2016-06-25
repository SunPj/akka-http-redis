name := """akka-http-redis"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.github.etaty" %% "rediscala" % "1.6.0")

// new repo on maven.org
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.6.0"
