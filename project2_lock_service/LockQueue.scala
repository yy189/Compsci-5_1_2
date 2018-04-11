package rings
import scala.collection.mutable
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

/**
  * Created by yxyang on 17/10/16.
  */
class LockQueue() {
  var holder:Int = -1
  var requester:mutable.Queue[Int] = new mutable.Queue[Int]()
  //last timestamp of the granted lock
  var timeLastAcquire = 0

}
