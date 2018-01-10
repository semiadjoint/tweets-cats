package com.example.tweetstat
package app

import scala.concurrent.ExecutionContext.Implicits.global
import cats.data._
import cats.effect._
import fs2._
object processing {
  /* This process pulls tweets from Twitter, analyzes each message
   * and produces a stream of diffs based on message analysis. Those
   * diffs are then used to update the above-mentioned stats signal.
   * */
  def apply(atomicStats: async.mutable.Signal[IO,AlgebirdStats]): Reader[App.Config, Stream[IO, StatsDiff]] = Reader { config =>
    val messages = TwitterMessageSource(config.twitter).messageStream
    val pps: Stream[IO, Stream[IO, StatsDiff]] =
      messages.map(x => processSingle(atomicStats)(x).run(config))

    // TODO read procs from config
    pps.join(scala.math.max(Runtime.getRuntime.availableProcessors, 8))
  }

  def processSingle(atomicStats: async.mutable.Signal[IO,AlgebirdStats])(message: Either[Err,Message]): Reader[App.Config,Stream[IO, StatsDiff]] = Reader { config =>
    val analysis = MessageAnalysis(config.analysis, config.stats)
    val handleMessageError: Sink[IO, Either[Err, Message]] = Sink { (e: Either[Err, Message]) =>
      e.fold(
        e => IO.apply(log.debug(s"Non-message event received: ${e.getMessage}")),
        _ => IO.unit
      )
    }
    Stream.eval(IO.pure(message))
      .observe(handleMessageError)
      .flatMap(e => e.fold(_ => Stream.emits(Seq.empty[Message]).covary[IO], r => Stream.emit(r).covary[IO]))
      .map(x => analysis.diff(x))
      .observe(analysis.sink(atomicStats))
      .logged(x => log.debug(x.toString))
  }


}
