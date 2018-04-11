package rings

import java.security.MessageDigest

import akka.actor.{Actor, Props}

import scala.concurrent.duration._

class Holder(var tranID: BigInt = -1, var readOrWrite: Int = -1)//which transaction takes this key and it's a read key or a write key
class Log(var operation: Operation, var commitOrAbort: Int)//1 for commit, 0 for abort

sealed trait KVStoreAPI
case class Put(key: BigInt, value: Any) extends KVStoreAPI
case class Get(key: BigInt) extends KVStoreAPI
case class Prepare(_i: Operation)
case class Commit(tranID: BigInt)
case class Abort(tranID: BigInt)
/**
 * KVStore is a local key-value store based on actors.  Each store actor controls a portion of
 * the key space and maintains a hash of values for the keys in its portion.  The keys are 128 bits
 * (BigInt), and the values are of type Any.
 */

class KVStore extends Actor {
  private val store = new scala.collection.mutable.HashMap[BigInt, Any]
  var log_i: Int = 0
  var lock = new scala.collection.mutable.HashMap[BigInt, Holder]
  var log: List[Log] = List()
  var hash = new scala.collection.mutable.HashMap[BigInt, Int]//For the convenience of finding the corresponding index to tranID in List log

  override def receive = {
    case Put(key, cell) =>
      sender ! store.put(key,cell)
    case Get(key) =>
      sender ! store.get(key)
    case Prepare(_i) =>
      sender ! Some(prepare(_i))
    case Commit(tranID) =>
      sender ! commit(tranID)
    case Abort(tranID) =>
      abort(tranID)
  }

  private def prepare(_i: Operation): Int = {
    var result: Int = -1
    hash.put(_i.tranID, log_i)
    log_i += 1
    log = (new Log(_i, -1))::log
    if(lock.contains(_i.key) == false || lock.get(_i.key).get.tranID == -1 || (lock.get(_i.key).get.readOrWrite == 0 && _i.readOrWrite == 0))
    //If nobody is occupying the lock or the lock being occupied is a read lock and the requested lock is also a read lock
    {
      lock.put(_i.key, new Holder(_i.tranID, _i.readOrWrite))//put the lock
      result = 1//vote yes
    }
    else//read write conflicts
    {
      import context.dispatcher
      context.system.scheduler.scheduleOnce(10 milliseconds) {
        //wait for 10 ms. After 10 ms, if can get the lock, vote yes; else, vote no
        if(lock.get(_i.key).get.tranID == -1)
        {
          lock.put(_i.key, new Holder(_i.tranID, _i.readOrWrite))
          result = 1
        }
        else
          result = 0
      }
    }
    return result
  }

  private def commit(tranID: BigInt): Any = {
    var ringCell: Any = new RingCell(0, 1)
    val i = hash.get(tranID).get
    val tran_i = log.apply(i).operation//get the corresponding operation from List log given tranID
    if(tran_i.readOrWrite == 0)//if it's read
      ringCell = store.get(tran_i.key)
    else {
      //if it's write
      ringCell = store.put(tran_i.key, tran_i.value)
      System.out.println("tranID: " + tranID + " put [" + tran_i.key + ", RingCell(" + tran_i.value.prev + ", " + tran_i.value.next + ")]")
    }
    log.apply(i).commitOrAbort = 1//write commit in log under that operation
    if(lock.get(tran_i.key).get.tranID == tranID)//release the lock
      lock.put(tran_i.key, new Holder(-1, -1))
    return ringCell
  }

  private def abort(tranID: BigInt) = {
    val i = hash.get(tranID).get
    val tran_i = log.apply(i).operation
    log.apply(i).commitOrAbort = 0
    if(lock.contains(tran_i.key) && (lock.get(tran_i.key).get.tranID == tranID))//If voted yes, release the lock
      lock.put(tran_i.key, new Holder(-1, -1))
  }
}

object KVStore {
  def props(): Props = {
     Props(classOf[KVStore])
  }
}
