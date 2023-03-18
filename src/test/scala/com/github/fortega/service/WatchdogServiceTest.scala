package com.github.fortega.service

import com.github.fortega.model.{Counter, Watchdog}
import org.scalatest.flatspec.AnyFlatSpec
import zio.stream.ZStream
import zio.{
  Duration,
  Exit,
  Ref,
  Runtime,
  Schedule,
  Unsafe,
  ZEnvironment,
  ZIO,
  ZLayer
}

class WatchdogServiceTest extends AnyFlatSpec {
  val layer = {
    val counter = ZLayer(Ref.make(Counter()))
    val watchdog = ZLayer(Ref.make(Watchdog(2)))
    counter ++ watchdog
  }
  val watchdogInterval = Duration.fromSeconds(1)
  val counterApp = for {
    refCounter <- ZIO.service[Ref[Counter]]
    result <- ZStream
      .range(0, 50)
      .schedule(Schedule.fixed(Duration.fromMillis(100)))
      .tap(_ => refCounter.update(_.increase))
      .runDrain
  } yield result

  def run[E, A](zio: ZIO[Any, E, A]): Exit[E, A] = Unsafe.unsafe {
    implicit unsafe =>
      Runtime.default.unsafe.run[E, A](zio)
  }

  "WatchdogService.create" should "activate on error" in {
    val app = counterApp raceFirst WatchdogService
      .createCounterIntervalSidecar(
        interval = watchdogInterval,
        min = Long.MaxValue
      )

    run(app.provideLayer(layer)) match {
      case Exit.Failure(cause) =>
        cause.failureOption match {
          case Some(watchdog) => assert(!watchdog.isValid)
          case None           => fail
        }
      case Exit.Success(_) => fail
    }
  }

  it should "end stream without error" in {
    val app = counterApp raceFirst WatchdogService
      .createCounterIntervalSidecar(interval = watchdogInterval)

    run(app.provideLayer(layer)) match {
      case Exit.Failure(_) => fail
      case Exit.Success(_) => succeed
    }
  }
}
