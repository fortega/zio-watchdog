package com.github.fortega.model

case class Counter(
    value: Long = 0
) {
  def increase = this.copy(value = value + 1)
  def reset = this.copy(value = 0)
}
