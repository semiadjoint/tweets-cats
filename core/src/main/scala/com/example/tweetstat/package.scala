package com.example

package object tweetstat {
  type Err = Throwable

  def Err(message: String) = new Throwable(message)
}
