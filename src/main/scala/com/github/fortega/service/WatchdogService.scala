package com.github.fortega.service

import com.github.fortega.model.{Counter, Watchdog}
import zio.{Duration, Ref, Schedule, ZIO}
import zio.stream.ZStream
import zio.ZLayer
object WatchdogService {

  /** Add a watchdog to a ZIO workflow usin a counter. Every time the interval
    * counter is get and reset from the shared workflow watchdog value is
    * update: if counter is 0 credits
    *
    * @param workflow
    *   workfow to race with
    * @param @param
    *   interval interval of execution
    * @param credit
    *   watdog credits
    * @return
    */
  def createCounterActivitySidecar[E <: Throwable, A](
      workflow: ZIO[Ref[Counter], E, A],
      interval: Duration,
      credit: Long
  ): ZIO[Any, Throwable, A] = {
    val watchdog = createWatchdog(
      interval = interval,
      credit = credit
    ).mapError(toThrowable)
    val result = (watchdog raceFirst workflow)
    val layer = {
      val counterLayer = ZLayer(Ref.make(Counter()))
      val watchdogLayer = ZLayer(Ref.make(Watchdog(credit)))
      counterLayer ++ watchdogLayer
    }
    result
      .mapAttempt { value =>
        require(value != null)
        value.asInstanceOf[A]
      }
      .provideLayer(layer)
  }

  /** Create watchdog ZIO
    *
    * @param interval
    *   interval of execution
    * @param credit
    *   watdog credits
    * @param min
    *   minium counter to declare error
    * @return
    */
  def createWatchdog(
      interval: Duration,
      credit: Long
  ): ZIO[Ref[Counter] with Ref[Watchdog], Watchdog, Unit] = for {
    refCounter <- ZIO.service[Ref[Counter]]
    refWatchDog <- ZIO.service[Ref[Watchdog]]
    result <- ZStream
      .fromSchedule(Schedule.fixed(interval))
      .mapZIO { _ =>
        for {
          counter <- refCounter.getAndUpdate(CounterService.reset)
          watchdog <- refWatchDog.updateAndGet(update(_, counter))
          _ <- ZIO.logDebug(s"$counter / $watchdog")
          validation <-
            if (isValid(watchdog)) ZIO.succeed(watchdog)
            else ZIO.fail(watchdog)
        } yield validation
      }
      .runDrain
  } yield result

  /** Is watchdog valid
    *
    * @param value
    *   watchdog
    * @return
    *   valid status
    */
  def isValid(value: Watchdog) = value.value > Watchdog.min

  /** Convert watchdog to throwable
    *
    * @param value
    *   current watchdow
    * @return
    *   throwable
    */
  def toThrowable(value: Watchdog) = new Throwable(value.toString)

  /** Validate the watchdog. If valid reset the value else is decreased
    *
    * @param value
    *   current watchdog
    * @return
    *   watchdog updated
    */
  def update(
      watchdog: Watchdog,
      counter: Counter
  ) =
    if (CounterService.hasActivity(counter)) reset(watchdog)
    else decrease(watchdog)

  /** Reset the watchdog. Value is the credits
    *
    * @param value
    *   current watchdog
    * @return
    *   watchdog updated
    */
  def reset(value: Watchdog) = value.copy(value = value.credits)

  /** Decreate the watchdog. value is decreased by one.
    *
    * @param value
    *   current watchdog
    * @return
    *   watchdow updated
    */
  def decrease(value: Watchdog) =
    value.copy(value = value.value - Watchdog.step)
}
