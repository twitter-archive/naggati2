import sbt._
import com.twitter.sbt._

class NaggatiProject(info: ProjectInfo) extends StandardProject(info) with DefaultRepos {
  val netty = "org.jboss.netty" % "netty" % "3.2.3.Final"
  val ostrich = "com.twitter" % "ostrich" % "3.0.4"

  // scala actors library with fork-join replaced by java 5 util.concurrent:
  // FIXME: we should investigate akka actors.
  val twitter_actors = "com.twitter" % "twitteractors_2.8.0" % "2.0.1"

  // for tests:
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.6" % "test"
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"
}
