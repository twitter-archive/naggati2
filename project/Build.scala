import sbt._
import Keys._
import com.twitter.sbt._

object Naggati extends Build {
  lazy val root = Project(
    id = "naggati",
    base = file("."),
    settings = Project.defaultSettings ++
      StandardProject.newSettings ++
      SubversionPublisher.newSettings
  ).settings(
    name := "naggati",
    organization := "com.twitter",
    version := "4.0.0",
    scalaVersion := "2.9.2",

    // time-based tests cannot be run in parallel
//    logBuffered in Test := false,
//    parallelExecution in Test := false,

    libraryDependencies ++= Seq(
      "com.twitter" % "util-core" % "5.0.0" % "provided",
      "io.netty" % "netty" % "3.3.1.Final" % "provided",

      // for tests only:
      "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test",
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
    ),

    SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")
  )
}