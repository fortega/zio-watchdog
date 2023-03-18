package com.github.fortega.service

import com.github.fortega.model.{Counter, Watchdog}
import zio.{Duration, Ref, Schedule, ZIO}
import zio.stream.ZStream
import zio.ZLayer
object WatchdogService {
  def createCounterActivitySidecar[E, A](
      zio: ZIO[Ref[Counter], E, A],
      interval: Duration,
      credit: Long,
      min: Long = 0
  ) = {
    val watchdog = for {
      refCounter <- ZIO.service[Ref[Counter]]
      refWatchDog <- ZIO.service[Ref[Watchdog]]
      result <- ZStream
        .fromSchedule(Schedule.fixed(interval))
        .mapZIO { _ =>
          for {
            counter <- refCounter.getAndUpdate(_.reset)
            watchdog <- refWatchDog.updateAndGet(
              _.check(counter.value > min)
            )
            _ <- ZIO.logDebug(s"$counter / $watchdog")
            validation <-
              if (watchdog.isValid) ZIO.succeed(watchdog)
              else ZIO.fail(watchdog)
          } yield validation
        }
        .runDrain
    } yield result
    val result = (watchdog raceFirst zio)
    val counterLayer = ZLayer(Ref.make(Counter()))
    val watchdogLayer = ZLayer(Ref.make(Watchdog(credit)))
    result.provideLayer(counterLayer ++ watchdogLayer)
  }
}
