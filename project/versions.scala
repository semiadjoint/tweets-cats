object V {
  lazy val scalatest = sbt.settingKey[String]("")
  lazy val scalacheck = sbt.settingKey[String]("")
  lazy val journal = sbt.settingKey[String]("")
  lazy val logback = sbt.settingKey[String]("")
  lazy val algebird = sbt.settingKey[String]("")

  lazy val cats = sbt.settingKey[String]("")
  lazy val circe = sbt.settingKey[String]("")
  lazy val catsEffect = sbt.settingKey[String]("")
  lazy val fs2 = sbt.settingKey[String]("")
  lazy val http4s = sbt.settingKey[String]("")
  lazy val doobie = sbt.settingKey[String]("")

  def catsEffectFrom(s: String): String = s match {
    case "1.0.1" => "0.7"
  }
  def doobieFrom(s: String): String = s match {
    case "0.10.0-M11" => "0.5.0-SNAPSHOT"
  }
  def http4sFrom(s: String): String = s match {
    case "0.10.0-M11" => "0.18.0-M8"
  }
  def fs2From(s: String): String = s match {
    case "0.7" => "0.10.0-M11"
  }
  def circeFrom(s: String): String = s match {
    case "1.0.1" => "0.9.0"
  }

}
