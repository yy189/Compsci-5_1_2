package rings

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.control._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

class AnyMap extends scala.collection.mutable.HashMap[BigInt, Any]

/**
 * KVClient implements a client's interface to a KVStore, with an optional writeback cache.
 * Instantiate one KVClient for each actor that is a client of the KVStore.  The values placed
 * in the store are of type Any: it is up to the client app to cast to/from the app's value types.
 * @param stores ActorRefs for the KVStore actors to use as storage servers.
 */

class KVClient (stores: Seq[ActorRef]) {
  private val cache = new AnyMap
  implicit val timeout = Timeout(5 seconds)

  import scala.concurrent.ExecutionContext.Implicits.global

  var dirtyset = new scala.collection.mutable.HashMap[BigInt, RingCell]

  def begin(transaction: List[Operation]) = {
    //Since every transaction has exactly 2 ops, at most 2 KVStores have to vote.
    //If one is read and the requested value is in cache, numVotes -= 1, which means
    //we no longer have to contact a KVStore to get that value
    var numVotes: Int = 2
    var count: Int = 0
    var set: Set[Int] = Set()
    for (i <- transaction) {
      if (i.readOrWrite == 0) //if it's read
      {
        val value = cache.get(i.key)
        if (value.isEmpty == false && dirtyset.contains(i.key) == false) {
          //if value is in cache and dirty bit == 0
          val value1 = value.get.asInstanceOf[RingCell]
          //get the value from cache
          System.out.println("Cache: RingCell (" + value1.prev + ", " + value1.next + ")")
          numVotes -= 1
          set = set + count
        }
        else//if cache doesn't have it
        {
          //ask this participant (KVStore) to vote
          val future = ask(route(i.key), Prepare(i)).mapTo[Option[Any]]
          //if time hasn't expired, we can get the vote result. We can only get Yes here.
          //Someone's voting no (can't get the lock before timeout or there exists a deadlock)
          //can be treated same as network partition (got no response)
          val yesOrNo = Await.result(future, timeout.duration).get
          //if this participant voted yes, we still needs numVote votes
          if (yesOrNo == 1) {
            numVotes -= 1
          }
        }
      }
      else//if it's write
      {
        //we write the data into cache and set the dirty bit to 1
        cache.put(i.key, i.value)
        dirtyset.put(i.key, i.value)
        val future = ask(route(i.key), Prepare(i)).mapTo[Option[Any]]
        val yesOrNo = Await.result(future, timeout.duration).get
        if (yesOrNo == 1) {
          numVotes -= 1
        }
      }
      count += 1
    }
    if(numVotes == 0)//If every participated KVStore voted yes, do commit; otherwise, do abort
      commit(transaction, set)
    else
      abort(transaction, set)
  }

  def commit(transaction: List[Operation], set: Set[Int]) = {
    var count: Int = 0
    for(i <- transaction) {
      if(set.contains(count) == false) {//if not cache
        if (i.readOrWrite == 0) //if it's read
        {
          //get the value from KVstore and put it into cache
          val future = ask(route(i.key), Commit(i.tranID)).mapTo[Option[Any]]
          val value = Await.result(future, timeout.duration).get.asInstanceOf[RingCell]
          cache.put(i.key, value)
          System.out.println("KVStore: RingCell (" + value.prev + ", " + value.next + ")")
        }
        else //if it's write
        {
          //ask the KVStore to write and set dirty bit to 0
          val future = ask(route(i.key), Commit(i.tranID)).mapTo[Option[Any]]
          dirtyset.remove(i.key)
        }
      }
      count += 1
    }
  }

  def abort(transaction: List[Operation], set: Set[Int]) = {
    var count: Int = 0
    for(i <- transaction) {
      if(set.contains(count) == false)
        route(i.key) ! Abort(i.tranID)
      count += 1
    }
  }

  /** Cached read */
  def read(key: BigInt): Option[Any] = {
    var value = cache.get(key)
    if (value.isEmpty) {
      value = directRead(key)
      if (value.isDefined)
        cache.put(key, value.get)
    }
    value
  }

  /** Cached write: place new value in the local cache, record the update in dirtyset. */
  def write(key: BigInt, value: Any, dirtyset: AnyMap) = {
    cache.put(key, value)
    dirtyset.put(key, value)
  }

  /** Push a dirtyset of cached writes through to the server. */
  def push(dirtyset: AnyMap) = {
    val futures = for ((key, v) <- dirtyset)
      directWrite(key, v)
    dirtyset.clear()
  }

  /** Purge every value from the local cache.  Note that dirty data may be lost: the caller
    * should push them.
    */
  def purge() = {
    cache.clear()
  }

  /** Direct read, bypass the cache: always a synchronous read from the store, leaving the cache unchanged. */
  def directRead(key: BigInt): Option[Any] = {
    val future = ask(route(key), Get(key)).mapTo[Option[Any]]
    Await.result(future, timeout.duration)
  }

  /** Direct write, bypass the cache: always a synchronous write to the store, leaving the cache unchanged. */
  def directWrite(key: BigInt, value: Any) = {
    val future = ask(route(key),Put(key,value)).mapTo[Option[Any]]
    Await.result(future, timeout.duration)
  }

  import java.security.MessageDigest

  /** Generates a convenient hash key for an object to be written to the store.  Each object is created
    * by a given client, which gives it a sequence number that is distinct from all other objects created
    * by that client.
    */
  /*def hashForKey(nodeID: Int, cellSeq: Int): BigInt = {
    val label = "Node" ++ nodeID.toString ++ "+Cell" ++ cellSeq.toString
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    val digest: Array[Byte] = md.digest(label.getBytes)
    BigInt(1, digest)
  }*/

  def hashForKey(key: Int): BigInt = {//对每把锁hash
    val label = "Key" ++ key.toString
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    val digest: Array[Byte] = md.digest(label.getBytes)
    BigInt(1, digest)
  }

  /**
    * @param key A key
    * @return An ActorRef for a store server that stores the key's value.
    */
  private def route(key: BigInt): ActorRef = {
    stores((key % stores.length).toInt)
  }
}
