package controllers

import scala.collection.mutable

object Stock {
  val stockList = mutable.Map[String, Stock](
      // Testing data
//	  "AEX.AS" -> Stock(0, "AEX.AS"), 
//	  "PNL.AS" -> Stock(1, "PNL.AS"),
//	  "FUR.AS" -> Stock(2, "FUR.AS"),
//	  "KPN.AS" -> Stock(3, "KPN.AS")
  )
  
  def stockListValuesSorted: Seq[Stock] = {Stock.stockList.values.toSeq.sortBy(_.symbol)}

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