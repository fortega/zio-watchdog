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
    val app = WatchdogService
      .createCounterActivitySidecar(
        zio = counterApp,
        interval = watchdogInterval,
        credit = 3,
        min = Long.MaxValue
      )

    run(app) match {
      case Exit.Failure(_) => succeed
      case Exit.Success(_) => fail
    }
  }

  it should "end stream without error" in {
    val app = WatchdogService
      .createCounterActivitySidecar(
        zio = counterApp,
        interval = watchdogInterval,
        credit = 3
      )

    run(app) match {
      case Exit.Failure(_) => fail
      case Exit.Success(_) => succeed
    }
  }
}
