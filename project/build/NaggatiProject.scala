import sbt._
import com.twitter.sbt._

class NaggatiProject(info: ProjectInfo) extends StandardLibraryProject(info)
  with SubversionPublisher
  with DefaultRepos
  with IdeaProject
{
  val netty = "org.jboss.netty" % "netty" % "3.2.3.Final"
  val util_core = "com.twitter" % "util-core" % "1.8.1"

  // for tests:
  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test"
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"

  override def subversionRepository = Some("http://svn.local.twitter.com/maven-public")
}
