package com.github.fortega.model

case class Watchdog(
    credits: Long,
    value: Long
)

object Watchdog {
  def apply(credits: Long): Watchdog = Watchdog(
    credits = credits,
    value = credits
  )
  val min = 0
  val step = 1
}
