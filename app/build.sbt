libraryDependencies += "io.verizon.knobs" %% "core" % "5.0.32"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % V.doobie.value,
  "org.tpolecat" %% "doobie-h2" % V.doobie.value
)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-blaze-client",
  "org.http4s" %% "http4s-circe",
  "org.http4s" %% "http4s-dsl"
).map(_ % V.http4s.value)
