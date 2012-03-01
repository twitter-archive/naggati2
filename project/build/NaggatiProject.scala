import sbt._
import com.twitter.sbt._

class NaggatiProject(info: ProjectInfo) extends StandardLibraryProject(info)
  with SubversionPublisher
  with DefaultRepos
{
  val netty = "org.jboss.netty" % "netty" % "3.3.1.Final"
  val util_core = "com.twitter" % "util-core_2.9.1" % "1.12.12"

  // for tests:
  val specs = "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"

  override def subversionRepository = Some("https://svn.twitter.biz/maven-public")
}
