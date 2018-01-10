lazy val root = {
  project
    .in(file("."))
    .dependsOn(app)
    .aggregate(core, app)
    .settings(aggregate.in(reStart) := false)
    .settings(
      V.scalatest in ThisBuild := "3.0.4",
      V.scalacheck in ThisBuild := "1.13.4",
      V.journal in ThisBuild := "3.0.19",
      V.logback in ThisBuild := "1.2.3",
      V.algebird in ThisBuild := "0.13.3",
      V.cats in ThisBuild := "1.0.1",
      V.circe in ThisBuild := V.circeFrom(V.cats.value),
      V.catsEffect in ThisBuild := V.catsEffectFrom(V.cats.value),
      V.fs2 in ThisBuild := V.fs2From(V.catsEffect.value),
      V.http4s in ThisBuild := V.http4sFrom(V.fs2.value),
      V.doobie in ThisBuild := V.doobieFrom(V.fs2.value)
    )
    .settings(
      mainClass in Global := Some("com.example.tweetstat.app.Main")
    )
    .settings(
      // Turn off compiler crankiness:
      // scalacOptions.in(core, Test) ~= filterConsoleScalacOptions
      scalacOptions.in(app, Compile) ~= filterConsoleScalacOptions
    )
    .settings(
      resolvers += Resolver.sonatypeRepo("releases"),
      addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.5"),
      addCommandAlias("build", ";test:compile"),
      addCommandAlias("rebuild", ";reload;build"),
      addCommandAlias("retest", ";reload;test"),
      organization := "com.example",
      name := "tweetstat",
      version := "0.0.1-SNAPSHOT",
      scalaVersion in ThisBuild := "2.12.4",
      scalafmtOnCompile := true
    )
}

lazy val core = {
  project
}

lazy val app = {
  project
    .dependsOn(core % "test->test;compile->compile")
    .enablePlugins(BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](name,
                                         version,
                                         scalaVersion,
                                         sbtVersion),
      buildInfoPackage := "buildinfo",
      buildInfoOptions += BuildInfoOption.BuildTime,
      buildInfoOptions += BuildInfoOption.ToJson
    )
}
