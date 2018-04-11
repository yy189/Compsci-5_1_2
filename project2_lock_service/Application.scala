package rings

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import scala.concurrent.duration._

sealed trait ApplicationAPI
case class Recall(namedLock: Int) extends ApplicationAPI
case class AcquireSucceed(nameLock: Int, currentTime: Int) extends ApplicationAPI

class Application (val appID: Int, lockServer: ActorRef, val numLocks: Int, burstSize: Int) extends Actor {

  val generator = new scala.util.Random
  var stats = new Stats
  var lockSet:Set[Int] = Set()

  def receive() = {
    case Command() =>
      incoming(sender)
      val namedLock = generator.nextInt(numLocks)
      acquire(namedLock) //loadmaster calls
    //command
    case Recall(namedLock) =>
      release(namedLock)
    case AcquireSucceed(namedLock, currentTime) =>
      acquireSucceed(namedLock, currentTime) //lockServer calls
  }

  /*private def command() = {
    val sample = generator.nextInt(100)
    if (sample < 70) {
      val namedLock = generator.nextInt(numLocks)
      lockClient.acquire(namedLock) //loadmaster calls
    } else {
      if(lockSet.size > 0)
        lockSet -= lockSet.head
      //lockClient.release(lockSet.head)//loadmaster calls
    }
  }*/

  /* private def recall(namedLock: Int) = {
     /*if (lockSet.contains(namedLock))
       lockSet -= namedLock*/
     lockClient.release(namedLock)
   }*/

  def acquire(namedLock: Int) = {
    if(!lockSet.contains(namedLock)) {
      lockServer ! Acquire(appID, namedLock)
      println(s"Application=$appID sent acquire request to the server (namedLock=$namedLock)")
    } else
      println(s"Named lock=$namedLock for application=$appID is in-cache")
  }

  def release(namedLock: Int) = {
    lockSet -= namedLock
    lockServer ! Release(appID, namedLock)
    println(s"Application=$appID sent release request to the server (namedLock=$namedLock)")
  }

  def acquireSucceed(namedLock: Int, validUntil: Int) = {
    lockSet += namedLock
    println(s"Acquiring named lock=$namedLock successfully for application=$appID")
    import context.dispatcher
    var Tnew = validUntil - System.currentTimeMillis().toInt
    //println(s"$Tnew.getClass")
    //FIXME Tnew < 0???
    if(Tnew>0) {
      context.system.scheduler.scheduleOnce(Tnew milliseconds) {
        if (lockSet.contains(namedLock)) {
          lockSet -= namedLock
          println(s"Named lock=$namedLock expired (appID=$appID).")
        }
      }
    }else{
      if (lockSet.contains(namedLock)) {
        lockSet -= namedLock
        println(s"Named lock=$namedLock expired (appID=$appID).")
      }
    }

  }

  private def incoming(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(appID, stats)
      stats = new Stats
    }
  }
}

object Application {
  def props(appID: Int, clientServer: ActorRef, numLocks: Int, burstSize: Int): Props = {
    Props(classOf[Application], appID, clientServer, numLocks, burstSize)
  }
}
