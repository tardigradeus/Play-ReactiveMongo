import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object Common extends AutoPlugin {
  // import com.typesafe.tools.mima.core._

  override def trigger = allRequirements
  override def requires = JvmPlugin

  val useShaded = settingKey[Boolean](
    "Use ReactiveMongo-Shaded (see system property 'reactivemongo.shaded')"
  )

  val driverVersion = settingKey[String]("Version of the driver dependency")

  override def projectSettings = Compiler.settings ++ Seq(
    organization := "org.reactivemongo",
    useShaded := sys.env.get("REACTIVEMONGO_SHADED").fold(true)(_.toBoolean),
    driverVersion := {
      val ver = (ThisBuild / version).value
      val suffix = {
        (useShaded.value, playVer.value startsWith "2.") match {
          case (false, false) => "noshaded.pekko"
          case (true, false) => "pekko"
          case (false, true) => "noshaded"
          case (true, true) => ""
        }
      }
      if (suffix.isEmpty) {
        ver
      } else {
        ver.span(_ != '-') match {
          case (_, "") => s"${ver}.${suffix}"

          case (a, b) => s"${a}.${suffix}${b}"
        }
      }
    },
    version ~= { ver =>
      val suffix = "play29"

      if (suffix.isEmpty) {
        ver
      } else {
        ver.span(_ != '-') match {
          case (_, "") => s"${ver}.${suffix}"

          case (a, b) => s"${a}.${suffix}${b}"
        }
      }
    },
    scalaVersion := "2.13.11",
    crossScalaVersions := Seq(
      "2.11.12",
      scalaVersion.value,
      "2.13.11",
      "3.2.2"
    ),
    crossVersion := CrossVersion.binary,
    Compile / compile / javacOptions ++= Seq(
      "-source",
      "1.8",
      "-target",
      "1.8"
    ),
    Compile / doc / sources := {
      val compiled = (Compile / doc / sources).value

      if (scalaBinaryVersion.value == "2.12") {
        compiled.filter { _.getName endsWith "NamedDatabase.java" }
      } else compiled
    },
    Compile / unmanagedSourceDirectories += {
      baseDirectory.value / "src" / "main" / playDir.value
    },
    Test / unmanagedSourceDirectories += {
      baseDirectory.value / "src" / "test" / playDir.value
    },
    Test / fork := false,
    Test / testOptions += Tests.Cleanup(cl => {
      import scala.language.reflectiveCalls
      val c = cl.loadClass("Common$")
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]
      m.close()
    })
  ) ++ Publish.settings ++ Publish.mimaSettings ++ Release.settings

  lazy val playLower = "2.5.0"
  lazy val playUpper = "2.8.21"

  lazy val playVer = Def.setting[String] {
    "3.0.2"
  }

  private lazy val playDir = Def.setting[String] {
    if (playVer.value startsWith "2.5") "play-2.6-"
    else s"play-${playVer.value take 3}"
  }
}
