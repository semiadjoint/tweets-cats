package com.example.tweetstat.app

import java.time.Instant

import journal.Logger
import cats.effect._
import fs2._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends StreamApp[IO] {
  val log = Logger[this.type]

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO,StreamApp.ExitCode] = for {
    config <- AppConfig.load

    app = App(config)
    s <- Stream.eval(app.atomicStats)
    t <- Stream.eval(IO.apply(Instant.now()))
    statting = processing(s).run(config)

    polling = app.startStatPolling(t, s)
    serving = app.startHttpServer(t, s.discrete)
    result <- serving
      .concurrently(statting)
      .concurrently(polling)
  } yield StreamApp.ExitCode.Success
}
