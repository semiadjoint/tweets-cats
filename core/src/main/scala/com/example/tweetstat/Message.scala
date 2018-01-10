package com.example.tweetstat

case class Hashtag(
    text: String
)

case class Url(
    displayUrl: String
)

case class Message(
    text: String,
    urls: List[Url],
    hashtags: List[Hashtag]
)
