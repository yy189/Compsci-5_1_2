package rings

import akka.actor.{Actor, ActorSystem, ActorRef, Props}
import akka.event.Logging

class RingCell(var prev: BigInt, var next: BigInt)
class RingMap extends scala.collection.mutable.HashMap[BigInt, RingCell]
class Operation(var tranID: BigInt, var key: BigInt, var value: RingCell, var readOrWrite: Int)

/**
 * RingService is an example app service for the actor-based KVStore/KVClient.
 * This one stores RingCell objects in the KVStore.  Each app server allocates new
 * RingCells (allocCell), writes them, and reads them randomly with consistency
 * checking (touchCell).  The allocCell and touchCell commands use direct reads
 * and writes to bypass the client cache.  Keeps a running set of Stats for each burst.
 *
 * @param myNodeID sequence number of this actor/server in the app tier
 * @param numNodes total number of servers in the app tier
 * @param storeServers the ActorRefs of the KVStore servers
 * @param burstSize number of commands per burst
 */

class RingServer (val myNodeID: Int, val numNodes: Int, storeServers: Seq[ActorRef], burstSize: Int) extends Actor {
  val generator = new scala.util.Random
  val cellstore = new KVClient(storeServers)
  val dirtycells = new AnyMap
  val localWeight: Int = 70
  val log = Logging(context.system, this)
  val numKeys: Int = 10

  var stats = new Stats
  var allocated: Int = 0
  var endpoints: Option[Seq[ActorRef]] = None


  def receive() = {
      case Prime() =>
        prestore
        //allocCell
      //     rwcheck(myNodeID, new RingCell(0,0))
      case Command() =>
        incoming(sender)
        //command
        transaction
      case View(e) =>
        endpoints = Some(e)
  }

  private def command() = {
    val sample = generator.nextInt(100)
    if (sample < 50) {
      allocCell
    } else {
      touchCell
    }
  }

  private def incoming(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(myNodeID, stats)
      stats = new Stats
    }
  }

  private def transaction() = {
    //Exactly 2 operations in each transaction,
    var transaction: List[Operation] = List()
    allocated = allocated + 1
    val tranID = cellstore.hashForKey(allocated)
    val sample = generator.nextInt(100)
    if (sample < 50) {
      transaction = prepareRead(tranID)::transaction
    } else {
      transaction = prepareWrite(tranID)::transaction
    }
    val sample1 = generator.nextInt(100)
    if(sample > 50) {
      transaction = prepareRead(tranID)::transaction
    } else {
      transaction = prepareWrite(tranID)::transaction
    }
    cellstore.begin(transaction)
  }

  private def prepareRead(tranID: BigInt): Operation = {//0 for read 1 for write
    val key = generator.nextInt(numKeys-1)
    val chosenKey = cellstore.hashForKey(key)
    return (new Operation(tranID, chosenKey, null, 0))
  }

  private def prepareWrite(tranID: BigInt): Operation = {//altogether numKeys keys
    val key = generator.nextInt(numKeys-1)
    val chosenKey = cellstore.hashForKey(key)
    val value = generator.nextInt(100)
    val r = new RingCell(value, value+1)
    return (new Operation(tranID, chosenKey, r, 1))
  }

  private def prestore() = {
    var i = 0;
    for(i <- 0 to numKeys-1)
      directWrite(cellstore.hashForKey(i), new RingCell(0, 1))
  }

  private def allocCell() = {
    val key = chooseEmptyCell
    var cell = directRead(key)
    assert(cell.isEmpty)
    val r = new RingCell(0, 1)
    stats.allocated += 1
    directWrite(key, r)
  }

  private def chooseEmptyCell(): BigInt =
  {
    allocated = allocated + 1
    cellstore.hashForKey(myNodeID)//, allocated)
  }

  /*
   * By modifying RingCells in place we may be racing with our k/v servers.  XXX
   */
  private def touchCell() = {
    stats.touches += 1
    val key = chooseActiveCell
    val cell = directRead(key)
    if (cell.isEmpty) {
      stats.misses += 1
    } else {
      val r = cell.get
      if (r.next != r.prev + 1) {
        stats.errors += 1
        r.prev = 0
        r.next = 1
      } else {
        r.next += 1
        r.prev += 1
      }
      directWrite(key, r)
    }
  }

  private def chooseActiveCell(): BigInt = {
    val chosenNodeID =
      if (generator.nextInt(100) <= localWeight)
        myNodeID
      else
        generator.nextInt(numNodes - 1)

    val cellSeq = generator.nextInt(allocated)
    cellstore.hashForKey(chosenNodeID)//, cellSeq)
  }

  private def rwcheck(key: BigInt, value: RingCell) = {
    directWrite(key, value)
    val returned = read(key)
    if (returned.isEmpty)
      println("rwcheck failed: empty read")
    else if (returned.get.next != value.next)
      println("rwcheck failed: next match")
    else if (returned.get.prev != value.prev)
      println("rwcheck failed: prev match")
    else
      println("rwcheck succeeded")
  }

  private def read(key: BigInt): Option[RingCell] = {
    val result = cellstore.read(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }

  private def write(key: BigInt, value: RingCell, dirtyset: AnyMap): Option[RingCell] = {
    val coercedMap: AnyMap = dirtyset.asInstanceOf[AnyMap]
    val result = cellstore.write(key, value, coercedMap)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }

  private def directRead(key: BigInt): Option[RingCell] = {
    val result = cellstore.directRead(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }

  private def directWrite(key: BigInt, value: RingCell): Option[RingCell] = {
    val result = cellstore.directWrite(key, value)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }

  private def push(dirtyset: AnyMap) = {
    cellstore.push(dirtyset)
  }
}

object RingServer {
  def props(myNodeID: Int, numNodes: Int, storeServers: Seq[ActorRef], burstSize: Int): Props = {
    Props(classOf[RingServer], myNodeID, numNodes, storeServers, burstSize)
  }
}
