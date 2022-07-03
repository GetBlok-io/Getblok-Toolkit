
name := "Getblok_Toolkit"
organization := "io.getblok"
version := "1.0"
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "4.0.9",
  "org.slf4j" % "slf4j-nop" % "1.7.36",
  "com.github.tototoshi" %% "scala-csv" % "1.3.10"
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)
assemblyJarName in assembly := s"GetblokToolkit.jar"
mainClass in assembly := Some("app.ToolkitApp")
assemblyOutputPath in assembly := file(s"./GetblokToolkit.jar/")

