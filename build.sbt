organization := "com.twitter"

name := "naggati"

version := "2.2.1-SNAPSHOT"

scalaVersion := "2.8.1"

resolvers += "twitter.com" at "http://maven.twttr.com"

resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven"

resolvers += "jboss" at "http://repository.jboss.org/nexus/content/repositories/releases/"

libraryDependencies += "com.github.mpeltonen" % "sbt-idea-plugin" % "0.3.0" 

libraryDependencies += "com.twitter" % "standard-project" % "0.11.1"

libraryDependencies += "org.jboss.netty" % "netty" % "3.2.5.Final"

libraryDependencies += "com.twitter" % "util-core" % "1.8.1"

libraryDependencies +=  "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test"

libraryDependencies +=  "org.jmock" % "jmock" % "2.4.0" % "test"

libraryDependencies +=  "org.hamcrest" % "hamcrest-all" % "1.1" % "test"

libraryDependencies +=  "cglib" % "cglib" % "2.1_3" % "test"

libraryDependencies +=  "asm" % "asm" % "1.5.3" % "test"

libraryDependencies +=  "org.objenesis" % "objenesis" % "1.1" % "test"

