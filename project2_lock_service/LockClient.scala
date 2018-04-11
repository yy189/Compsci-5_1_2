package rings

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.concurrent.duration._

class LockClient (val appID: Int, val lockServer: ActorRef) extends Actor{
  //cache
  var lockSet:Set[Int] = Set()

  def receive() ={
    case AcquireSucceed(nameLock: Int, currentTime: Int)=>

  }

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
    context.system.scheduler.scheduleOnce(validUntil - System.currentTimeMillis() milliseconds) {
      if(lockSet.contains(namedLock)) {
        lockSet -= namedLock
        println(s"Named lock=$namedLock expired (appID=$appID).")
      }
    }

  }

  /*def recall(namedLock: Int) = {
    lockSet.remove(namedLock)
    println(s"NamedLock=$namedLock recalled from application=$appID")
  }*/


  /*var messages: Int = 0
  var allocated: Int = 0
  var checks: Int = 0
  var touches: Int = 0
  var misses: Int = 0
  var errors: Int = 0

  def += (right: Stats): Stats = {
    messages += right.messages
    allocated += right.allocated
    checks += right.checks
    touches += right.touches
    misses += right.misses
    errors += right.errors
	  this
  }

  override def toString(): String = {
    s"Stats msgs=$messages alloc=$allocated checks=$checks touches=$touches miss=$misses err=$errors"
  }*/
}
