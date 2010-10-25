import sbt._
import com.twitter.sbt._

class NaggatiProject(info: ProjectInfo) extends StandardProject(info) with DefaultRepos {
  val netty = "org.jboss.netty" % "netty" % "3.2.2"

  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test"

  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"  //--auto--
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"  //--auto--
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"  //--auto--
  val asm = "asm" % "asm" % "1.5.3" % "test"  //--auto--
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"  //--auto--
}
