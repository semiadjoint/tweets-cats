package com.example.tweetstat
package app

import journal.Logger
import org.http4s._
import org.http4s.client.{Client, oauth1}
import cats.effect._
import fs2.Stream
import jawnfs2._
import _root_.io.circe.{Decoder, HCursor, Json}
import com.example.tweetstat.Message

object TwitterMessageSource {
  object Config {
    case class Secrets(consumerSecret: Secret, tokenSecret: Secret)
  }
  case class Config(client: Client[IO],
                    consumerKey: String,
                    consumerSecret: Secret,
                    token: String,
                    tokenSecret: Secret)

  implicit val decodeUrl: Decoder[Url] = new Decoder[Url] {
    final def apply(c: HCursor): Decoder.Result[Url] =
      for {
        displayUrl <- c.downField("display_url").as[String]
      } yield Url(displayUrl)
  }
  implicit val decodeHashtags: Decoder[Hashtag] = new Decoder[Hashtag] {
    final def apply(c: HCursor): Decoder.Result[Hashtag] =
      for {
        displayHashtag <- c.downField("display_url").as[String]
      } yield Hashtag(displayHashtag)
  }
  implicit val decodeMessage: Decoder[Message] = new Decoder[Message] {
    final def apply(c: HCursor): Decoder.Result[Message] =
      for {
        text <- c.downField("text").as[String]
        urls <- c.downField("entities").downField("urls").as[List[Url]]
        hashtags <- c
          .downField("entities")
          .downField("hashtags")
          .as[List[Hashtag]]
      } yield Message(text, urls, hashtags)
  }
}
case class TwitterMessageSource(config: TwitterMessageSource.Config) {
  val log = Logger[this.type]

  val client = config.client

  val request = Request[IO](
    Method.GET,
    //TODO move into config
    Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

  // TODO handle reconnects and delayed connections
  def byteStream: EntityBody[IO] =
    for {
      sr <- Stream.eval(
        oauth1.signRequest[IO](
          request,
          oauth1.Consumer(
            config.consumerKey,
            config.consumerSecret.unsafeExtractClearText
          ),
          None,
          None,
          Some(
            oauth1.Token(config.token,
                         config.tokenSecret.unsafeExtractClearText))
        )
      )
      res <- client.streaming(sr)(resp => resp.body)
    } yield res

  implicit val f = _root_.io.circe.jawn.CirceSupportParser.facade

  def messageStream: Stream[IO, Either[Err, Message]] = {
    byteStream.chunks.parseJsonStream
      .map { json =>
        json
          .as[Message](TwitterMessageSource.decodeMessage)
          .fold(x => Left(Err(s"${x.message}: ${json.toString}")), Right.apply)
      }
  }
}
