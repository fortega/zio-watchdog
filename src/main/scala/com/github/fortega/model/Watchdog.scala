package com.github.fortega.model

case class Watchdog(
    value: Long,
    credits: Long
) {
  lazy val isValid = value > 0

  def check(
      succeed: => Boolean
  ): Watchdog =
    if (succeed) this.copy(value = credits)
    else this.copy(value = value - 1)
}

object Watchdog {
  def apply(credits: Long): Watchdog = Watchdog(
    value = credits,
    credits = credits
  )
}
