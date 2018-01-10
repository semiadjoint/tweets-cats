package com.example.tweetstat.app

import java.time.Instant

import cats.data._
import cats.effect._
import com.example.tweetstat.AlgebirdStats
import fs2._

import scala.concurrent.ExecutionContext.Implicits.global

object polling {

  /* This process periodically polls the above-mentioned stats signal,
   * according to the cadence specified in the config. This produces a
   * stream of stats summaries, with lossy-histogram size as specified in
   * the config.
   * */
  def apply(start: Instant, atomicStats: async.immutable.Signal[IO,AlgebirdStats])
  : Reader[App.Config, Stream[IO, AlgebirdStats.Summary]] = Reader { config =>
    for {
      stats <- config.scheduler.fixedRate[IO](config.pollerCadence)
        .zip(atomicStats.continuous).map(_._2)
      now <- Stream.eval(IO.apply(Instant.now))
      summary = stats.summary(config.pollerHistogramSize,
        finiteDuration(start, now))
    } yield summary
  }

}
