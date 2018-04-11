package rings

import akka.actor.{ActorSystem, ActorRef, Props}

sealed trait AppServiceAPI
case class Prime() extends AppServiceAPI
case class Command() extends AppServiceAPI
case class View(endpoints: Seq[ActorRef]) extends AppServiceAPI

/**
  * This object instantiates the service tiers and a load-generating master, and
  * links all the actors together by passing around ActorRef references.
  *
  * The service to instantiate is bolted into the KVAppService code.  Modify this
  * object if you want to instantiate a different service.
  */

object KVAppService {

  def apply(system: ActorSystem, numLocks:Int, T: Int, numNodes: Int, ackEach: Int): ActorRef = {

    val lockServer = system.actorOf(LockServer.props(numNodes, numLocks, T, ackEach), "LockServer")

    val applications = for(i <- 0 until numNodes)
      yield system.actorOf(Application.props(i, lockServer, numLocks, ackEach), "Application" + i)

    /*/** Storage tier: create K/V store servers */
    val stores = for (i <- 0 until numNodes)
      yield system.actorOf(KVStore.props(), "RingStore" + i)

    /** Service tier: create app servers */
    val servers = for (i <- 0 until numNodes)
      yield system.actorOf(RingServer.props(i, numNodes, stores, ackEach), "RingServer" + i)

    /** If you want to initialize a different service instead, that previous line might look like this:
      * yield system.actorOf(GroupServer.props(i, numNodes, stores, ackEach), "GroupServer" + i)
      * For that you need to implement the GroupServer object and the companion actor class.
      * Following the "rings" example.
      */


    /** Tells each server the list of servers and their ActorRefs wrapped in a message. */*/
    lockServer ! View(applications)

    /** Load-generating master */
    val master = system.actorOf(LoadMaster.props(numNodes, applications, lockServer, ackEach), "LoadMaster")
    master
  }
}

