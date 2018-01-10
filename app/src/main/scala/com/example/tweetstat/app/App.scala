package com.example.tweetstat.app

import java.time.Instant

import cats.effect._
import fs2._
import com.example.tweetstat._
import journal.Logger
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

object App {
  case class Config(
    scheduler: Scheduler,
    serverPort: Int,
    twitter: TwitterMessageSource.Config,
    analysis: MessageAnalysis.Config,
    stats: AlgebirdStats.Config,
    pollerCadence: FiniteDuration,
    pollerHistogramSize: Int,
    banner: Vector[String] = banner
  )

  val banner =
    """
      |
      |  _                     _       _        _
      | | |___      _____  ___| |_ ___| |_ __ _| |_ ___
      | | __\ \ /\ / / _ \/ _ \ __/ __| __/ _` | __/ __|
      | | |_ \ V  V /  __/  __/ |_\__ \ || (_| | |_\__ \
      |  \__| \_/\_/ \___|\___|\__|___/\__\__,_|\__|___/
      |
      |
  """.stripMargin.split('\n').toVector


}

case class App(config: App.Config) {
  val log = Logger[this.type]


  /* This signal gets updated asynchronously in a background task,
   * and exposes a stream of stats that can be polled by a periodic
   * logging task or by HTTP requests. See below for examples of each.
   * */
  val atomicStats: IO[async.mutable.Signal[IO, AlgebirdStats]] =
    async.signalOf[IO,AlgebirdStats](AlgebirdStats.empty(config.stats.maxCounters))


  /* Start background job that pulls tweets and updates stats. */
  def startMessageProcessing(atomicStats: async.mutable.Signal[IO,AlgebirdStats]): Stream[IO,StatsDiff] =
    processing(atomicStats).run(config)

  /* Start background job to periodically log stats. */
  def startStatPolling(start: Instant,
                       atomicStats: async.immutable.Signal[IO,AlgebirdStats]): Stream[IO,AlgebirdStats.Summary] =
    polling(start, atomicStats).run(config)
      .logged(s => log.info(s))

  def startHttpServer(startTime: Instant, stats: Stream[IO, AlgebirdStats])
  : Stream[IO, StreamApp.ExitCode] = {
    BlazeBuilder[IO]
      .withBanner(config.banner)
      .bindHttp(config.serverPort)
      .mountService(HttpHandlers(startTime, stats).service, "/tweetstat/")
      .serve
  }

}
