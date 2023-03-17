package com.github.fortega.service

import org.scalatest.flatspec.AnyFlatSpec
import zio.stream.ZStream
import zio.Schedule
import zio.Duration
import zio.Runtime
import zio.Unsafe
import zio.ZIO
import zio.Exit
import zio.Exit.Failure
import zio.Exit.Success
import zio.ZEnvironment
import zio.Ref
import zio.ZLayer
import com.github.fortega.model.Counter
import com.github.fortega.model.Watchdog

case object TimeoutError extends Throwable

class WatchdogServiceTest extends AnyFlatSpec {
  val layer = {
    val counter = ZLayer(Ref.make(Counter()))
    val watchdog = ZLayer(Ref.make(Watchdog(2)))
    counter ++ watchdog
  }
  val counterApp = for {
    refCounter <- ZIO.service[Ref[Counter]]
    result <- ZStream
      .range(0, 50)
      .schedule(Schedule.fixed(Duration.fromMillis(100)))
      .tap(_ => refCounter.update(_.increase))
      .runDrain
  } yield result

  val watchdogInterval = Duration.fromSeconds(1)

  def run[E, A](zio: ZIO[Any, E, A]): Exit[E, A] = Unsafe.unsafe {
    implicit unsafe =>
      Runtime.default.unsafe.run[E, A](zio)
  }

  def accumMeasure(counter: Counter) =
    counter.value * 1.0 / watchdogInterval.toSeconds

  "WatchdogService.create" should "activate on error" in {

    val app = counterApp raceFirst WatchdogService
      .create[Double](
        interval = watchdogInterval,
        measureValidation = _ => false,
        accumMeasure = accumMeasure
      )

    run(app.provideLayer(layer)) match {
      case Failure(cause) =>
        cause.failureOption match {
          case Some(watchdog) => assert(!watchdog.isValid)
          case None           => fail
        }
      case Success(_) => fail
    }
  }

  it should "end stream without error" in {
    val app = counterApp raceFirst WatchdogService
      .create[Double](
        interval = watchdogInterval,
        measureValidation = _ => true,
        accumMeasure = accumMeasure
      )

    run(app.provideLayer(layer)) match {
      case Failure(_) => fail
      case Success(_) => succeed
    }
  }
}
