
object Meta {
  lazy val strict = sbt.settingKey[Boolean]("")
  lazy val empty = sbt.settingKey[Unit]("")
}
