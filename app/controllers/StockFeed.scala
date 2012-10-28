package controllers

import scala.collection.mutable
import scala.io.Source
import java.net.URL
import play.api.Logger

object StockFeed {
  val yahooRegex = """([\^\w\.]*)",([\d\.]*),"([\d\/]*)","([\dapm:]*)",([\+-\.\d]*),([\d\.]*),([\d\.]*),([\d\.]*),([\d]*).*""".r

  def getQuotes(stockList:mutable.Map[String, Stock]): Set[Stock] = {
    val source = Source.fromInputStream(
        new URL("http://download.finance.yahoo.com/d/quotes.csv?s=" + stockList.keys.mkString(",") + "&f=sl1d1t1c1ohgv&e=.csv")
        	.openConnection()
        	.getInputStream()
    )

    source.getLines.filter(!_.isEmpty).map {
	    yahooRegex.findFirstIn(_) match {
	      case Some(yahooRegex(symbol, priceString, date, time, change, open, high, low, volume)) => {
	        val price = java.lang.Double.parseDouble(priceString)
	        // Get from the map
	    	// Is the value different?
	        stockList.get(symbol).filter(!_.price.equals(price))
	        	.map { stock =>
	        		// Up or down?
	        	  	if(stock.price.compareTo(price) < 0)
	        	  		new Stock(stock.id, symbol, price, "+")
	        	  	else
	        	  		new Stock(stock.id, symbol, price, "-")
	        	}
	      }
	      case None => {
	        Logger.warn("Failed to parse response: " + source.mkString)
	        None
	      }
	    }      
    }.filter(!_.isEmpty).map(_.get).toSet
  }
}