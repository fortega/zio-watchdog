package com.github.fortega.service

import com.github.fortega.model.{Counter, Watchdog}
import org.scalatest.flatspec.AnyFlatSpec
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
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.Fiber

object WatchdogServiceTest {
  val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  val watchdogInterval = Duration.fromMillis(20)
  val counterInterval = Duration.fromMillis(25)
  val counterTo = 4
  val expectedLast = counterTo - 1
  val counterApp = for {
    refCounter <- ZIO.service[Ref[Counter]]
    result <- ZStream
      .range(0, counterTo)
      .schedule(Schedule.fixed(counterInterval))
      .tap(_ => refCounter.update(CounterService.increase))
      .runFold(0) { case (_, v) => v }
  } yield result
  val sleepApp = for {
    refCounter <- ZIO.service[Ref[Counter]]
    _ <- Fiber.never.join
  } yield expectedLast

  def run[E, A](zio: ZIO[Any, E, A]): Exit[E, A] = Unsafe.unsafe {
    implicit unsafe =>
      Runtime.default.unsafe.run[E, A](zio)
  }
}

class WatchdogServiceTest extends AnyFlatSpec {
  import WatchdogServiceTest._

  "WatchdogService.create" should "activate on error" in {
    val app = WatchdogService
      .createCounterActivitySidecar(
        workflow = sleepApp,
        interval = watchdogInterval,
        credit = 3
      )
      .provideLayer(logger)

    run(app) match {
      case Exit.Failure(cause) =>
        cause.failureOption match {
          case None        => fail
          case Some(error) => succeed
        }
        succeed
      case Exit.Success(last) =>
        println(last)
        fail
    }
  }

  it should "end stream without error" in {
    val app = WatchdogService
      .createCounterActivitySidecar(
        workflow = counterApp,
        interval = watchdogInterval,
        credit = 3
      )
      .provideLayer(logger)

    run(app) match {
      case Exit.Failure(_)    => fail
      case Exit.Success(last) => assert(last == expectedLast)
    }
  }
}
