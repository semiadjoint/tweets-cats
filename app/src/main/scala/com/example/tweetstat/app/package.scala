package com.example.tweetstat

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import journal.Logger

import cats.effect._
import fs2._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

package object app {
  val log = Logger[this.type]

  implicit class StreamOps[A](p: Stream[IO, A]) {
    def logged(log: String => Unit): Stream[IO, A] = {
      p.observe(Sink((x: A) => IO.apply(log(x.toString))))
    }
  }
  def finiteDuration(start: Instant, end: Instant): FiniteDuration =
    FiniteDuration(Duration.between(start, end).toNanos, TimeUnit.NANOSECONDS)
}
