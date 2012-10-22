package controllers

import scala.collection.mutable

import java.net.URL
import play.api.mvc.Action
import play.api.mvc.Controller
import scala.io.Source
import play.api.Logger
import play.api.data._
import play.api.data.Forms._

case class Stock(id: Int, symbol: String, price: java.lang.Double, direction: String)

object Application extends Controller {
  val yahooRegex = """([\^\w\.]*)",([\d\.]*),"([\d\/]*)","([\dapm:]*)",([\+-\.\d]*),([\d\.]*),([\d\.]*),([\d\.]*),([\d]*).*""".r
   
  val stockList = mutable.Map[String, Stock]()
      
  val stockListForm = Form(
		  single("stockList" -> nonEmptyText)
  )
	
  def init = Action {
    Ok(views.html.init(stockListForm))
  }
  
  def updateInit = Action { implicit request =>
    stockListForm.bindFromRequest.fold(
	  errors => BadRequest(views.html.init(errors)),
	  value => { // binding success, you get the actual value
		  stockList.clear
		  var index = 0
		  value.split(",").foreach { symbol => 
		    stockList.put(symbol, new Stock(index, symbol, 0D, "/")) 
		  	index += 1 
		  }
	    
		  Redirect(routes.Application.poll)
	  } 
	)
  }  
  
  def poll = Action {
    val source = Source.fromInputStream(
        new URL("http://download.finance.yahoo.com/d/quotes.csv?s=" + stockList.keys.mkString(",") + "&f=sl1d1t1c1ohgv&e=.csv")
        	.openConnection()
        	.getInputStream()
    )
    
    val stocks = source.getLines.filter(!_.isEmpty).map {
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
	        	}.filter { stock =>
	        	  // Replace the stock with the latest value
	        	  stockList.put(stock.symbol, stock)
	        	  true
	      		}
	      }
	      case None => {
	        Logger.warn("Failed to parse response: " + source.mkString)
	        None
	      }
	    }      
    }.filter(!_.isEmpty).map(_.get)
    
    Ok(views.html.poll(stocks))
  }
}