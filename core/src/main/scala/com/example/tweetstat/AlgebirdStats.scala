package com.example.tweetstat

import com.twitter.algebird.SpaceSaver

import scala.concurrent.duration.FiniteDuration

object AlgebirdStats {
  case class Config(maxCounters: Int)

  case class Summary(
      totalMessageCount: Long,
      emojiMessageCount: Long,
      urlMessageCount: Long,
      photoUrlMessageCount: Long,
      topEmoji: Seq[(String, Long, Boolean)],
      topHashtags: Seq[(String, Long, Boolean)],
      topUrlDomains: Seq[(String, Long, Boolean)],
      topPhotoUrlDomains: Seq[(String, Long, Boolean)],
      averageHour: Long,
      averageMin: Long,
      averageSec: Long
  ) {
    override def toString: String = {
      s"""Summary(
    |  totalMessageCount=$totalMessageCount,
    |  emojiMessageCount=$emojiMessageCount,
    |  urlMessageCount=$urlMessageCount,
    |  photoUrlMessageCount=$photoUrlMessageCount,
    |  topEmoji=$topEmoji,
    |  topHashtags=$topHashtags,
    |  topUrlDomains=$topUrlDomains,
    |  topPhotoUrlDomains=$topPhotoUrlDomains,
    |  averageHour=$averageHour,
    |  averageMin=$averageMin,
    |  averageSec=$averageSec
    |)
       """.stripMargin
    }
  }
  def emptySpaceSaver(maxCounters: Int) = SpaceSaver(maxCounters, "", 0L)
  def empty(maxCounters: Int): AlgebirdStats =
    AlgebirdStats(
      totalMessageCount = 0L,
      emojiMessageCount = 0L,
      urlMessageCount = 0L,
      photoUrlMessageCount = 0L,
      emojiHistogram = emptySpaceSaver(maxCounters),
      hashtagHistogram = emptySpaceSaver(maxCounters),
      urlDomainHistogram = emptySpaceSaver(maxCounters),
      photoUrlDomainHistogram = emptySpaceSaver(maxCounters)
    )
}
case class AlgebirdStats(
    totalMessageCount: Long,
    emojiMessageCount: Long,
    urlMessageCount: Long,
    photoUrlMessageCount: Long,
    emojiHistogram: SpaceSaver[String],
    hashtagHistogram: SpaceSaver[String],
    urlDomainHistogram: SpaceSaver[String],
    photoUrlDomainHistogram: SpaceSaver[String]
) {
  def summary(k: Int, elapsed: FiniteDuration): AlgebirdStats.Summary = {
    val nanos = elapsed.toNanos
    val seconds = nanos / 1e9
    val minutes = seconds / 60.0
    val hours = minutes / 60.0
    AlgebirdStats.Summary(
      totalMessageCount,
      emojiMessageCount,
      urlMessageCount,
      photoUrlMessageCount,
      topEmoji = emojiHistogram.topK(k).map {
        case (emoji, approx, b) => (emoji, approx.estimate, b)
      },
      topHashtags = hashtagHistogram.topK(k).map {
        case (hashtag, approx, b) => (hashtag, approx.estimate, b)
      },
      topUrlDomains = urlDomainHistogram.topK(k).map {
        case (urlDomain, approx, b) => (urlDomain, approx.estimate, b)
      },
      topPhotoUrlDomains = photoUrlDomainHistogram.topK(k).map {
        case (urlDomain, approx, b) => (urlDomain, approx.estimate, b)
      },
      averageHour = math.round(totalMessageCount / hours),
      averageMin = math.round(totalMessageCount / minutes),
      averageSec = math.round(totalMessageCount / seconds)
    )
  }
}
