package rings

import akka.actor.{Actor, ActorSystem, ActorRef, Props}
import akka.event.Logging
import scala.concurrent.duration._

sealed trait LockServerAPI
case class Acquire(appID: Int, namedLock: Int) extends LockServerAPI
case class Release(appID: Int, namedLock: Int) extends LockServerAPI

class LockServer (val myNodeID:Int, val numLocks: Int, val T: Int, burstSize: Int) extends Actor {

  val generator = new scala.util.Random
  var stats = new Stats
  val log = Logging(context.system, this)
  var fail: Int = 0
  var recall_0: Int = 0
  var namedLock_0: Int = 0
  var numMessage: Int = 0
  //var a:Array[Int] = new Array[Int](numLocks)
  var applications: Option[Seq[ActorRef]] = None
  var lockQueue = new Array[LockQueue](numLocks)


  def receive() = {
    case Prime() =>
      initializeA
    case Acquire(appID, namedLock) =>
      acquire(appID, namedLock)
    case Release(appID, namedLock) =>
      release(appID, namedLock)
    case Command() =>
      incoming(sender)
      failure()


    case View(e) =>
      applications = Some(e)
  }

  private def incoming(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(myNodeID, stats)
      stats = new Stats
    }
  }

  private def initializeA() = {
    for(i <- 0 until numLocks) {
      //a(i) = -1
      lockQueue(i) = new LockQueue()
    }
  }

  private def acquire(appID: Int, namedLock: Int) = {
    var head: Int = 0
    var flag:Int = 0
    if(appID == 0 && fail == 0) {
      numMessage += 1
    }
    val currentTime = System.currentTimeMillis().toInt
    if(lockQueue(namedLock).holder == -1) {//no one use this lock
      lockQueue(namedLock).holder = appID
      lockQueue(namedLock).timeLastAcquire = currentTime
      applications.get(appID) ! AcquireSucceed(namedLock, currentTime + T)
    }
    else {//someone is holdign this lock

      if(!lockQueue(namedLock).requester.nonEmpty)//isEmpty
        head = appID
      else
        lockQueue(namedLock).requester.enqueue(appID)
      if(fail == 0 || lockQueue(namedLock).holder != 0){
        applications.get(lockQueue(namedLock).holder) ! Recall(namedLock)
        applications.get(head) ! AcquireSucceed(namedLock, currentTime + T)
        lockQueue(namedLock).holder = appID
        lockQueue(namedLock).timeLastAcquire = currentTime
        if(lockQueue(namedLock).requester.nonEmpty)
          head = lockQueue(namedLock).requester.dequeue()
      }
      else {
        val waitUntil = lockQueue(namedLock).timeLastAcquire + T - currentTime
        if (waitUntil < 0) {//has expired
          applications.get(head) ! AcquireSucceed(namedLock, currentTime + T)
          lockQueue(namedLock).holder = appID
          lockQueue(namedLock).timeLastAcquire = currentTime
          if(lockQueue(namedLock).requester.nonEmpty)
            head = lockQueue(namedLock).requester.dequeue()
        }
        else {
          if (flag == 0) {
            flag = 1
            import context.dispatcher
            context.system.scheduler.scheduleOnce(waitUntil milliseconds) {
              applications.get(head) ! AcquireSucceed(namedLock, currentTime + T)
              lockQueue(namedLock).holder = appID
              lockQueue(namedLock).timeLastAcquire = currentTime
              if (lockQueue(namedLock).requester.nonEmpty)
                head = lockQueue(namedLock).requester.dequeue()
              flag = 0
            }
          }
        }
      }
    }
    /*if(appID != 0 || fail == 0)
      if(appID == 0)
        numMessage += 1
      val temp = a(namedLock)
      if(temp == -1) {
        a(namedLock) = appID
        applications.get(appID) ! AcquireSucceed(namedLock)
      } else
        if(temp != 0 || fail == 0)
          applications.get(temp) ! Recall(namedLock)
        else {
          recall_0 = 1
          namedLock_0 = namedLock
        }
        a(namedLock) = appID
        applications.get(appID) ! AcquireSucceed(namedLock)*/

  }

  private def release(appID: Int, namedLock: Int) = {
    if(appID == 0 && fail == 0)
      numMessage += 1
    println(s"Application=$appID released NamedLock=$namedLock")
    /*if(appID != 0 || fail == 0)
      if(appID == 0)
        numMessage += 1
      println(s"Application=$appID released NamedLock=$namedLock")*/
  }

  private def failure() = {
    val pro = generator.nextInt(100)
    if(pro < 101)
      fail = 1
    println(s"Failure begins with message number=$numMessage")
    import context.dispatcher
    context.system.scheduler.scheduleOnce(T seconds) {
      fail = 0
      println(s"Failure ends with message number=$numMessage")
      if(recall_0 == 1)
        applications.get(0) ! Recall(namedLock_0)
      recall_0 = 0
    }
  }

  private def print() = {
    println(s"message number=$numMessage")
  }
}


object LockServer {
  def props(myNodeID:Int, numLocks: Int, T: Int, burstSize: Int): Props = {
    Props(classOf[LockServer], myNodeID, numLocks, T, burstSize)
  }
}
