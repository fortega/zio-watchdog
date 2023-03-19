package com.github.fortega.service

import com.github.fortega.model.Counter

object CounterService {

  /** Return true if the counter is increased after the last reset
    *
    * @param value
    *   counter
    * @return
    *   true if has activity else false
    */
  def hasActivity(value: Counter) = value.value > Counter.min

  /** Increase the counter. Used in main workflow to inform activity
    * @param value
    *   current value
    * @return
    *   value plus one
    */
  def increase(value: Counter) = value.copy(value = value.value + Counter.step)

  /** Reset the counter to 0. Used in watchdow to restart interval.
    * @param value
    *   current value
    * @return
    *   value = 0
    */
  def reset(value: Counter) = value.copy(value = Counter.min)
}
