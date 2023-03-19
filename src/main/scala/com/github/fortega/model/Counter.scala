package com.github.fortega.model

case class Counter(
    value: Long = Counter.min
)

object Counter {
  val min = 0
  val step = 1
}
