package com.example.tweetstat.app

import java.net.URL
import java.nio.file.Path

import com.example.tweetstat.{AlgebirdStats, Err, MessageAnalysis}
import _root_.io.circe
import _root_.io.circe.{Decoder, HCursor, parser}
import journal.Logger

import scala.concurrent.duration.FiniteDuration
import knobs.{ClassPathResource, Required}
import cats.effect._
import fs2._
import scala.concurrent.ExecutionContext.Implicits.global

object AppConfig {
  val log = Logger[this.type]

  /* The following task loads our secrets from the specified file.
   * The loaded `knobs.Config` contains our cleartext secrets.
   * To find use sites where secrets are again printable,
   * search for uses of the `unsafeExtractClearText` method.
   * */
  def loadTwitterSecrets(
      path: String): IO[TwitterMessageSource.Config.Secrets] =
    for {
      secrets <- knobs.loadImmutable[IO](
        List(Required(ClassPathResource(path))))
      result = TwitterMessageSource.Config.Secrets(
        tokenSecret = Secret(secrets.require[String]("twitter.tokenSecret")),
        consumerSecret =
          Secret(secrets.require[String]("twitter.consumerSecret"))
      )
    } yield result

  def load: Stream[IO, App.Config] =
    for {
      defaultsCfg <- Stream.eval(
        knobs.loadImmutable[IO](
          List(Required(ClassPathResource("defaults.cfg")))))
      twitterSecrets <- Stream.eval(
        loadTwitterSecrets(defaultsCfg.require[String]("twitter.secrets-file")))

      //todo convert into Path
      emojiDathFile = defaultsCfg.require[String]("message-analysis.data-file")
      emojiData <- loadEmojiData(emojiDathFile).map(_.fold(throw _, identity))
      client <- Stream.eval(org.http4s.client.blaze.Http1Client[IO]())
      scheduler <- Scheduler[IO](corePoolSize = 8) //todo this should come from config
    } yield
      App.Config(
        scheduler = scheduler,
        serverPort = defaultsCfg.require[Int]("server.port"),
        twitter = TwitterMessageSource.Config(
          //todo decdie what to do about client
          client = client,
          consumerKey = defaultsCfg.require[String]("twitter.consumer-key"),
          consumerSecret = twitterSecrets.consumerSecret,
          token = defaultsCfg.require[String]("twitter.token"),
          tokenSecret = twitterSecrets.tokenSecret
        ),
        analysis = MessageAnalysis.Config(
          emojiData = emojiData,
          picDomains =
            defaultsCfg.require[List[String]]("message-analysis.pic-domains")
        ),
        stats = AlgebirdStats.Config(
          defaultsCfg.require[Int]("stats.histogram-max-counters")
        ),
        pollerCadence = defaultsCfg.require[FiniteDuration]("poller.cadence"),
        pollerHistogramSize = defaultsCfg.require[Int]("poller.top-k-size")
      )

  implicit val decodeMessage: Decoder[(String, String)] =
    new Decoder[(String, String)] {
      final def apply(c: HCursor): Decoder.Result[(String, String)] = {
        for {
          unicodeHex <- c.downField("unified").as[String]
          name <- Right(c.downField("name").as[String].getOrElse(unicodeHex))
        } yield unicodeHex -> name
      }

    }

  def loadEmojiData(
      path: String): Stream[IO, Either[Err, Map[String, String]]] = {
    val p: Path =
      java.nio.file.Paths.get(getClass.getResource("/" + path).getPath)
    // todo there's got to be a better way than folding all the strings
    val value: Stream[IO, String] = fs2.io.file
      .readAllAsync[IO](p, 4096)
      .through(text.utf8Decode).fold("")(_ + _)
    //todo clean up this mess
    for {
      mtxt <- value.attempt
      result = for {
        txt <- mtxt.swap.map(x => Err(x.getMessage)).swap
        x <- parser
          .parse(txt)
          .swap
          .map((x: circe.ParsingFailure) => Err(x.message))
          .swap
        res <- x
          .as[List[(String, String)]]
          .swap
          .map((x: circe.Error) => Err("Could not decode emoji json to List"))
          .swap
      } yield res.toMap

    } yield result
  }

}
