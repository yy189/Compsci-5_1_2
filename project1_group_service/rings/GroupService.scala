package rings

import akka.actor.{Actor, ActorSystem, ActorRef, Props}
import akka.event.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

class GroupCell(var prev: Option[BigInt], var next: Option[BigInt], var serverID: Option[Int],var cellID: BigInt)
//class RingMap extends scala.collection.mutable.HashMap[BigInt, RingCell]

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

class GroupServer (val myNodeID: Int, val numNodes: Int, val numGroups: Int, storeServers: Seq[ActorRef], burstSize: Int) extends Actor {
  val generator = new scala.util.Random
  val cellstore = new KVClient(storeServers)
  val dirtycells = new AnyMap
  val localWeight: Int = 70
  val log = Logging(context.system, this)
  var stats = new Stats
  var endpoints: Option[Seq[ActorRef]] = None
  var allocated: Int = 0

  // new variables which we defined
  var groupArray = Array.fill(numGroups)(0)

  def receive() = {
    case Prime() =>
    //rwcheck(myNodeID, new RingCell(0,0))
      initializeGroups
    case Command() =>
      incoming(sender)
      command

    case View(e) =>
      endpoints = Some(e)

    case JoinG(groupID,nid) =>
      writeJoin(groupID,nid)
      println(nid.toString ++ " join group" ++ groupID.toString)
      readGroup(groupID)
    case LeaveG(groupID,nid) =>
      writeLeave(groupID,nid)
      println(nid.toString ++ " leave group" ++ groupID.toString)
      readGroup(groupID)

    case SendMulticast(groupID,ttl) =>
      //println("to send message to group"++groupID.toString)
      sendMulticast(groupID,ttl)

    case ReceiveMulticast(ttl) =>
      //println("receive  message")
      receiveMulticast(ttl)
  }

  //define message types
  private def initializeGroups() = {
    for(i <- 0 until numGroups){
      //I am the leader for group i
      //so I write the head of group i into KV store
      if(i%numNodes==myNodeID){
        val key=groupIDtoBigInt(i)
        val r=new GroupCell(None,None,None,key)
        directWrite(key,r)
      }
    }
  }

  private def command() = {
    val sample = generator.nextInt(60)
    if (sample < 20) {
      JoinGroup
    } else if (sample < 40) {
      LeaveGroup
    } else {
      SendMessage
    }
  }

  private def JoinGroup()={
    stats.joins += 1
    val groupID = chooseGroupID(0)
    //println("choosen id" ++ groupID.toString)
    if(groupID != -1){
      //println(endpoints.get.length)
      endpoints.get(groupID%numNodes) ! JoinG(groupID,myNodeID)
      //println(myNodeID.toString ++ "want to join group" ++ groupID.toString)
      changeGroupArray(groupID,0)
    }
  }

  private def LeaveGroup()={
    stats.leaves += 1
    val groupID = chooseGroupID(1)
    //println("choosen id" ++ groupID.toString)
    if(groupID != -1){
      endpoints.get(groupID%numNodes) ! LeaveG(groupID,myNodeID)
      //println(myNodeID.toString ++ "want to leave group" ++ groupID.toString)
      changeGroupArray(groupID,1)
    }
  }

  private def SendMessage()={
    val groupID = chooseGroupID(1)
    if(groupID != -1){
      //println("to send message")
      endpoints.get(groupID%numNodes)!SendMulticast(groupID,1)
    }
  }

  private def incoming(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(myNodeID, stats)
      stats = new Stats
    }
  }

  /*private def allocCell() = {
    val key = chooseEmptyCell
    var cell = directRead(key)
    assert(cell.isEmpty)
    val r = new RingCell(0, 1)
    stats.allocated += 1
    directWrite(key, r)
  }*/

  private def chooseGroupID(method:Int):Int = {
    //FIXME: choose a random group to join/leave
    //groupArray.indexOf(method)
    var joinedN:Int=0
    var ret=
      for(i <- 0 until numGroups){
        if(groupArray(i)==1)
          joinedN +=1
      }

    if(method==0 && joinedN<numGroups) {
      var start:Int=generator.nextInt(numGroups)
      var flag:Boolean=true
      while(flag){
        start+=1
        if(groupArray(start%numGroups)==method){
          flag=false
        }
      }
      start%numGroups

    }else if(method==1 && joinedN>0){
      var start:Int=generator.nextInt(numGroups)
      var flag:Boolean=true
      while(flag){
        start+=1
        if(groupArray(start%numGroups)==method){
          flag=false
        }
      }
      start%numGroups

    }else{
      -1
    }

  }

  private def chooseEmptyCell(): BigInt =
  {
    allocated = allocated + 1
    cellstore.hashForKey(myNodeID, allocated)
  }


  private def chooseActiveCell(): BigInt = {
    val chosenNodeID =
      if (generator.nextInt(100) <= localWeight)
        myNodeID
      else
        generator.nextInt(numNodes - 1)

    val cellSeq = generator.nextInt(allocated)
    cellstore.hashForKey(chosenNodeID, cellSeq)
  }

  private def changeGroupArray(groupID:Int,method:Int) = {
    if(method==0) {
      groupArray(groupID) = 1
    }
    else{
      groupArray(groupID) = 0
    }
  }
  /*
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

  private def read(key: BigInt): Option[GroupCell] = {
    val result = cellstore.read(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }

  private def write(key: BigInt, value: RingCell, dirtyset: AnyMap): Option[GroupCell] = {
    val coercedMap: AnyMap = dirtyset.asInstanceOf[AnyMap]
    val result = cellstore.write(key, value, coercedMap)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[RingCell])
  }
  */

  //FIXME hash GroupID to a BigInt in a fixed way
  import java.security.MessageDigest
  private def groupIDtoBigInt(groupID:Int):BigInt={
    val label = "group" ++ groupID.toString
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    val digest: Array[Byte] = md.digest(label.getBytes)
    BigInt(1, digest)
  }

  private def directRead(key: BigInt): Option[GroupCell]= {
    val result = cellstore.directRead(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[GroupCell])
  }

  private def directWrite(key: BigInt, value: GroupCell): Option[GroupCell] = {
    val result = cellstore.directWrite(key, value)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[GroupCell])
  }

  //this function is only for debug,delete it when submitting homework
  private def readGroup(groupID:Int)={
    //println("current members in group "++groupID.toString)
    val key = groupIDtoBigInt(groupID)
    var cell = directRead(key)
    while(!cell.get.next.isEmpty){
        cell = directRead(cell.get.next.get)
        //println(cell.get.serverID)
    }
  }
  private def writeJoin(groupID: Int,nid:Int)={
    val key = groupIDtoBigInt(groupID)
    var cell = directRead(key)
    var found:Boolean=false
    while(!cell.get.next.isEmpty && !found){
        cell = directRead(cell.get.next.get)
        if(cell.get.serverID.get==nid){
          found=true
        }
    }
    //append new groupCell after cell
    if(!found){
      val nextkey=cell.get.next;
      val cellID = chooseEmptyCell()
      val newcell=new GroupCell(Some(cell.get.cellID),nextkey,Some(nid),cellID)
      //We need to write all modified cell back
      directWrite(cellID,newcell)
      val c=cell.get
      c.next=Some(cellID)
      directWrite(c.cellID,c)
      if(!nextkey.isEmpty){
        var nextcell = directRead(nextkey.get)
        if(!nextcell.isEmpty){
          var nc=nextcell.get
          nc.prev=Some(cellID)
          directWrite(nextkey.get,nc)
        }
      }
    }

    stats.joins += 1
  }

  private def writeLeave(groupID:Int, nid:Int)={
    val key = groupIDtoBigInt(groupID)
    var cell = directRead(key)
    var found:Boolean=false
    while(!cell.get.next.isEmpty && !found){
        cell = directRead(cell.get.next.get)
        if(cell.get.serverID.get == nid){
          found=true
        }
    }
    //append new groupCell after cell
    if(found){
      val nextkey=cell.get.next;
      val prevkey=cell.get.prev;
      var prevcell=directRead(prevkey.get).get
      prevcell.next=nextkey
      directWrite(prevkey.get,prevcell)
      if(!nextkey.isEmpty){
        var nextcell = directRead(nextkey.get)
        if(!nextcell.isEmpty){
          var nc=nextcell.get
          nc.prev=prevkey
          directWrite(nextkey.get,nc)
        }
      }
    }
    stats.leaves += 1
  }

  private def sendMulticast(groupID:Int, ttl:Int)={
    //key:Option[BigInt]
    var key=directRead(groupIDtoBigInt(groupID)).get.next
    var targetServers: Option[Seq[ActorRef]] = None
    while(!key.isEmpty){
      //c:cell
      val c=directRead(key.get).get
      endpoints.get(c.serverID.get)!ReceiveMulticast(ttl)
      stats.send += 1
      //println("sent to server " ++ c.serverID.get.toString)
      key = c.next
    }
  }

  private def receiveMulticast(ttl:Int) ={
    //println(myNodeID.toString ++ " receive messages")
    stats.receive+= 1
    if(ttl>0){
      val groupID = chooseGroupID(1)
      if(groupID != -1){
        endpoints.get(groupID%numNodes) ! sendMulticast(groupID, ttl-1)
      }
    }
  }
  /*
  private def push(dirtyset: AnyMap) = {
    cellstore.push(dirtyset)
  }
  */
  //FIXME forward to be implemented
  //private def route()
}

object GroupServer {
  def props(myNodeID: Int, numNodes: Int, numGroups: Int, storeServers: Seq[ActorRef], burstSize: Int): Props = {
    Props(classOf[GroupServer], myNodeID, numNodes,numGroups, storeServers, burstSize)
  }
}
