package com.example.tweetstat.app

import java.time.Instant

import cats.effect._
import fs2._
import com.example.tweetstat.AlgebirdStats
import _root_.io.circe._
import _root_.io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s._
import org.http4s.headers.`Content-Type`

case class HttpHandlers(start: Instant,
                                 stats: Stream[IO, AlgebirdStats]) {
  def errorJson(s: String): Json =
    Json.obj("error" -> Json.fromString(s))

  def service = HttpService[IO] {
    case GET -> Root =>
      Ok(buildinfo.BuildInfo.toJson)
        .map(_.withContentType(`Content-Type`(MediaType.`application/json`)))

    case GET -> Root / "status" =>
      Ok("up")

    case GET -> Root / "stats" / IntVar(k) =>
      import _root_.io.circe.syntax._
      for {
        now <- IO.apply(Instant.now)
        result <- stats
          .take(1)
          .map(_.summary(k, finiteDuration(start, now)).asJson)
          .map(Ok(_))
          .compile.last.map(_.fold(NotFound(errorJson("No stats found")))(identity[IO[Response[IO]]]))
        result <- result
      } yield result
  }
}
