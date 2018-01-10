package com.example.tweetstat

import com.twitter.algebird.SpaceSaver

import cats.data._
import cats.effect._
import fs2._

object MessageAnalysis {
  case class Config(
      picDomains: List[String],
      emojiData: Map[String, String]
  )
}
case class MessageAnalysis(config: MessageAnalysis.Config,
                           statConfig: AlgebirdStats.Config) {
  def diff(m: Message): StatsDiff =
    statsDiff(m, statConfig.maxCounters)
      .run(config.picDomains)
      .run(config.emojiData)

  type EMoggi[A] = Reader[Map[String, String], A] // Sorry :)
  type KUrls[A] = Reader[List[String], A]

  val hashtagRegex = """#[a-zA-Z][a-zA-Z0-9]*""".r

  // TODO handle non-ascii hashtags
  def collectHashtags(m: Message): Vector[String] = {
    hashtagRegex.findAllIn(m.text).toVector
  }

  def collectEmoji(m: Message): EMoggi[Vector[String]] = Reader.apply {
    emojiData =>
      m.text
        .map(c => emojiData.get(c.toLong.toHexString.toUpperCase))
        .toVector
        .flatten
  }

  def collectUrls(m: Message): Vector[String] = {
    m.urls.toVector.map(_.displayUrl)
  }

  def collectPhotoUrlDomains(
      urlDomains: Vector[String]): KUrls[Vector[String]] = Reader { photoUrls =>
    urlDomains.filter(photoUrls.contains(_))
  }

  def getDomain(s: String): String = {
    s.takeWhile(_ != '/')
  }

  def photoUrlMessageCountDiff(photoUrls: Vector[String]): Long = {
    if (photoUrls.isEmpty) 0
    else 1
  }

  def urlMessageCountDiff(urls: Vector[String]): Long = {
    if (urls.isEmpty) 0
    else 1
  }

  val totalCountDiff: Long = 1

  def emojiHistoDiff(capacity: Int,
                     emojis: Vector[String]): SpaceSaver[String] = {
    if (emojis.isEmpty) AlgebirdStats.emptySpaceSaver(capacity)
    else
      emojis.foldLeft(AlgebirdStats.emptySpaceSaver(capacity))((a, b) =>
        a ++ SpaceSaver(capacity, b))

  }

  def hashtagHistogramDiff(capacity: Int, hashtags: Vector[String]) = {
    if (hashtags.isEmpty) AlgebirdStats.emptySpaceSaver(capacity)
    else
      hashtags.foldLeft(AlgebirdStats.emptySpaceSaver(capacity))(
        _ ++ SpaceSaver(capacity, _))
  }

  def urlDomainHistogramDiff(capacity: Int, urls: Vector[String]) = {
    if (urls.isEmpty) AlgebirdStats.emptySpaceSaver(capacity)
    else
      urls.foldLeft(AlgebirdStats.emptySpaceSaver(capacity))(
        _ ++ SpaceSaver(capacity, _))
  }

  def photoUrlDomainHistogramDiff(
      capacity: Int,
      photoUrls: Vector[String]): SpaceSaver[String] = {
    if (photoUrls.isEmpty) AlgebirdStats.emptySpaceSaver(capacity)
    else
      photoUrls.foldLeft(AlgebirdStats.emptySpaceSaver(capacity))(
        _ ++ SpaceSaver(capacity, _))
  }

  def emojiMessageCountDiff(emojis: Vector[String]): Long = {
    if (emojis.isEmpty) 0
    else 1
  }

  def statsDiff(m: Message, capacity: Int): KUrls[EMoggi[StatsDiff]] = {
    Reader { urlCfg =>
      Reader { emojiData =>
        val hashtags = collectHashtags(m)
        val emojis = collectEmoji(m).run(emojiData)
        val urlDomains = collectUrls(m).map(getDomain)
        val photoUrlDomains = collectPhotoUrlDomains(urlDomains).run(urlCfg)

        StatsDiff(
          totalMessageCountDiff = totalCountDiff,
          emojiMessageCountDiff = emojiMessageCountDiff(emojis),
          urlMessageCountDiff = urlMessageCountDiff(urlDomains),
          photoUrlMessageCountDiff = photoUrlMessageCountDiff(photoUrlDomains),
          emojiHistogramDiff = emojiHistoDiff(capacity, emojis),
          hashtagHistogramDiff = hashtagHistogramDiff(capacity, hashtags),
          urlDomainHistogramDiff = urlDomainHistogramDiff(capacity, urlDomains),
          photoUrlDomainHistogramDiff =
            photoUrlDomainHistogramDiff(capacity, photoUrlDomains)
        )
      }

    }

  }

  def updateStats(stats: AlgebirdStats,
                  m: Message): KUrls[EMoggi[AlgebirdStats]] =
    Reader { urlCfg =>
      Reader { emojiData =>
        val hashtags = collectHashtags(m)
        val emojis = collectEmoji(m).run(emojiData)
        val urlDomains = collectUrls(m).map(getDomain)
        val photoUrlDomains = collectPhotoUrlDomains(urlDomains).run(urlCfg)

        val updatedStats =
          AlgebirdStats(
            totalMessageCount = stats.totalMessageCount + totalCountDiff,
            emojiHistogram = stats.emojiHistogram ++ emojiHistoDiff(
              stats.emojiHistogram.capacity,
              emojis),
            emojiMessageCount = stats.emojiMessageCount + emojiMessageCountDiff(
              emojis),
            hashtagHistogram = stats.hashtagHistogram ++ hashtagHistogramDiff(
              stats.hashtagHistogram.capacity,
              hashtags),
            urlMessageCount = stats.urlMessageCount + urlMessageCountDiff(
              urlDomains),
            photoUrlMessageCount = stats.photoUrlMessageCount + photoUrlMessageCountDiff(
              photoUrlDomains),
            urlDomainHistogram = stats.urlDomainHistogram ++ urlDomainHistogramDiff(
              stats.urlDomainHistogram.capacity,
              urlDomains),
            photoUrlDomainHistogram = stats.photoUrlDomainHistogram ++ photoUrlDomainHistogramDiff(
              stats.photoUrlDomainHistogram.capacity,
              photoUrlDomains)
          )
        updatedStats
      }

    }

  def applyDiff(stats: AlgebirdStats, diff: StatsDiff): AlgebirdStats = {
    AlgebirdStats(
      totalMessageCount = stats.totalMessageCount + diff.totalMessageCountDiff,
      emojiHistogram = stats.emojiHistogram ++ diff.emojiHistogramDiff,
      emojiMessageCount = stats.emojiMessageCount + diff.emojiMessageCountDiff,
      hashtagHistogram = stats.hashtagHistogram ++ diff.hashtagHistogramDiff,
      urlMessageCount = stats.urlMessageCount + diff.urlMessageCountDiff,
      photoUrlMessageCount = stats.photoUrlMessageCount + diff.photoUrlMessageCountDiff,
      urlDomainHistogram = stats.urlDomainHistogram ++ diff.urlDomainHistogramDiff,
      photoUrlDomainHistogram = stats.photoUrlDomainHistogram ++ diff.photoUrlDomainHistogramDiff
    )
  }

  def sink(stats: async.mutable.Signal[IO, AlgebirdStats]): Sink[IO, StatsDiff] = {
    Sink { (diff: StatsDiff) =>
      stats.modify(applyDiff(_, diff)).map(_ => ())
    }
  }
}

case class StatsDiff(
    totalMessageCountDiff: Long,
    emojiMessageCountDiff: Long,
    urlMessageCountDiff: Long,
    photoUrlMessageCountDiff: Long,
    emojiHistogramDiff: SpaceSaver[String],
    hashtagHistogramDiff: SpaceSaver[String],
    urlDomainHistogramDiff: SpaceSaver[String],
    photoUrlDomainHistogramDiff: SpaceSaver[String]
)
