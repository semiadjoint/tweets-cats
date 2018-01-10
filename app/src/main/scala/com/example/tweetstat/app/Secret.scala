package com.example.tweetstat.app

case class Secret(unsafeExtractClearText: String) {
  override def toString: String = "Secret(<redacted>)"

}
