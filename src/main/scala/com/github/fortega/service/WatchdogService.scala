package com.github.fortega.service

import com.github.fortega.model.{Counter, Watchdog}
import izumi.reflect.Tag
import zio.{Duration, Ref, Schedule, ZIO}
import zio.stream.ZStream

object WatchdogService {
  def create[Measure](
      interval: Duration,
      measureValidation: Measure => Boolean,
      accumMeasure: Counter => Measure
  )(implicit
      measureTag: Tag[Measure]
  ) = for {
    refCounter <- ZIO.service[Ref[Counter]]
    refWatchDog <- ZIO.service[Ref[Watchdog]]
    result <- ZStream
      .fromSchedule(Schedule.fixed(interval))
      .mapZIO { _ =>
        for {
          counter <- refCounter.getAndUpdate(_.reset)
          meassure = accumMeasure(counter)
          watchdog <- refWatchDog.updateAndGet(
            _.check(measureValidation(meassure))
          )
          _ <- ZIO.log(s"counter: $counter / measure: $meassure / watchdog: $watchdog")
          validation <-
            if (watchdog.isValid) ZIO.succeed(meassure)
            else ZIO.fail(watchdog)
        } yield validation
      }
      .runDrain
  } yield result
}
