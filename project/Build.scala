import sbt._
import Keys._
//import com.twitter.sbt._

object Naggati extends Build {
  lazy val root = Project(
    id = "naggati",
    base = file("."),
    settings = Project.defaultSettings //++
//      StandardProject.newSettings ++
//      SubversionPublisher.newSettings
  ).settings(
    name := "naggati",
    organization := "com.twitter",
    version := "4.1.2-SNAPSHOT",
    scalaVersion := "2.10.2",

    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % "6.2.4" % "provided",
      "io.netty" % "netty" % "3.5.12.Final" % "provided",

      // for tests only:
      "org.specs2" %% "specs2" % "2.3.10" % "test",
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
    )

//    SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")
  )
}
