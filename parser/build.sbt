
name := "streaming-json"

version := "1.0"

organization := "Lunatech labs"

logLevel := Level.Error

val jacksonVersion = "2.6.3"

val poiVersion = "3.13"

val scalazVersion = "7.1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-effect" % scalazVersion,
  "org.apache.poi" % "poi" % poiVersion,
  "org.apache.poi" % "poi-ooxml" % poiVersion,
  "org.apache.poi" % "poi-ooxml-schemas" % poiVersion,
  "org.apache.poi" % "poi-scratchpad" % poiVersion,
  "org.apache.xmlbeans" % "xmlbeans" % "2.6.0",
  "com.typesafe.play" %% "play-json" % "2.3.6"    
)



scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  //"-Xfatal-warnings",
  //"-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  //"-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-language:postfixOps",
  "-language:implicitConversions",
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)