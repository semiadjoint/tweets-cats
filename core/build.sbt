libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % V.scalatest.value % "test",
  "org.scalacheck" %% "scalacheck" % V.scalacheck.value % "test"
)
libraryDependencies ++= Seq(
  "io.verizon.journal" %% "core" % V.journal.value,
  "ch.qos.logback" % "logback-classic" % V.logback.value
)

libraryDependencies += "com.twitter" %% "algebird-core" % V.algebird.value

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % V.circe.value)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"
).map(_ % V.cats.value)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect"
).map(_ % V.catsEffect.value)

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core",
  "co.fs2" %% "fs2-io"
).map(_ % V.fs2.value)
