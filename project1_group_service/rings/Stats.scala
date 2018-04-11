package rings

class Stats {
  var messages: Int = 0
  var joins: Int=0
  var leaves: Int=0
  var send:Int=0
  var receive: Int=0

  def += (right: Stats): Stats = {
    messages += right.messages
    joins += right.joins
    leaves += right.leaves
    send += right.send
    receive += right.receive
	  this
  }

  override def toString(): String = {
    s"Stats msgs=$messages join=$joins leaves=$leaves send=$send receive=$receive"
  }
}
