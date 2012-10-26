package controllers

import scala.collection.mutable

object Stock {
  val stockList = mutable.Map[String, Stock]("AEX.AS" -> Stock(0, "AEX.AS"), "FUR.AS" -> Stock(1, "FUR.AS"))

  def apply(id: Int, fullSymbol: String) = {
    new Stock(id, fullSymbol, 0D, "/")
  }
}
case class Stock(id: Int, fullSymbol: String, price: java.lang.Double, direction: String) extends Ordered[Stock] {
  val symbol = if(fullSymbol.indexOf('.') >= 0) {
	  fullSymbol.take(fullSymbol.indexOf('.'))
  }
  else {
    fullSymbol
  }
  def compare(other:Stock) = symbol compare other.symbol
}