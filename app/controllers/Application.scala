package controllers

import java.net.URL
import scala.collection.mutable
import scala.io.Source
import akka.util.duration.intToDurationInt
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.tuple
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Enumerator.Pushee
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import scala.collection.immutable.TreeSet

object Stock {
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

case class StockPushee(pushee:Pushee[String], var count: Int = 0)

object Application extends Controller {
  val yahooRegex = """([\^\w\.]*)",([\d\.]*),"([\d\/]*)","([\dapm:]*)",([\+-\.\d]*),([\d\.]*),([\d\.]*),([\d\.]*),([\d]*).*""".r
   
  var update = true
  val stockList = mutable.Map[String, Stock]("AEX.AS" -> Stock(0, "AEX.AS"), "FUR.AS" -> Stock(1, "FUR.AS"))
  val stockPushees = new mutable.HashSet[StockPushee]() with mutable.SynchronizedSet[StockPushee]

  Akka.system.scheduler.schedule(0 seconds, 2 seconds) {
    if(!stockPushees.isEmpty) {
    	Logger.info("Processing pushees...")
    	processPushees
    }
  }
  
  val stockListForm = Form(
		  tuple("stockList" -> nonEmptyText,
		      "update" -> boolean)
  )
	
  def init = Action {
    Ok(views.html.init(stockList.values, stockListForm))
  }
  
  def updateInit = Action { implicit request =>
    stockListForm.bindFromRequest.fold(
	  errors => BadRequest(views.html.init(stockList.values, errors)),
	  value => { // binding success, you get the actual value
		  stockList.clear
		  var index = 0
		  value._1.split(",").foreach { symbol => 
		    stockList.put(symbol, new Stock(index, symbol, 0D, "/")) 
		  	index += 1 
		  }
		  
		  update = value._2
	    
		  Redirect(routes.Application.init)
	  } 
	)
  }  
  
  def poll = Action {
    Ok(views.html.poll(getQuotes))
  }
  
  def getQuotes = {
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
	        	  	val newStock = if(stock.price.compareTo(price) < 0)
	        	  		new Stock(stock.id, symbol, price, "+")
	        	  	else
	        	  		new Stock(stock.id, symbol, price, "-")
	        	  	
	        	  	if(update) { 
	        	  	  stockList.put(stock.fullSymbol, newStock) 
	        	  	}
	        	  	
	        	  	newStock
	        	}
	      }
	      case None => {
	        Logger.warn("Failed to parse response: " + source.mkString)
	        None
	      }
	    }      
    }.filter(!_.isEmpty).map(_.get)
  }
  
  def pollStream = Action {
	  val dataStream = Enumerator.pushee[String] (
		  	onStart = { pushee =>
		  	  // Add to the list to process
		  	  stockPushees.add(new StockPushee(pushee))
		  	  
		  	  // Push everything the first time a client connects
		  	  // TODO Sort the list
		  	  pushee.push(views.html.poll(stockList.valuesIterator).toString.trim)
		  	},
		  	onComplete = { Logger.info("Complete") },
		  	onError = { (error, input) => 
		  	  Logger.error("Error with stream: " + error + " input: " + input) 
		  	}
	  )

	  Ok.stream(dataStream)
  }
  
  	// if you want to directly serialize/deserialize, you need to write yourself a formatter right now
	implicit object StockFormat extends Format[Stock] {
	    def reads(json: JsValue): Stock = Stock((json \ "id").as[Int], (json \ "symbol").as[String])
	    def writes(stock: Stock): JsValue = JsObject(
	        List("id" -> JsNumber(stock.id),
	        		"symbol" -> JsString(stock.symbol),
	        		"price" -> JsNumber(new java.math.BigDecimal(stock.price.toString)),
	        		"direction" -> JsString(stock.direction)
	        	)
	    )
	
	}

  def pollStreamJson = Action {
    // TODO Sort the list
    Ok(Json.toJson(stockList.values.toSeq))
  }
  
  def processPushees = {
    val quotes = getQuotes
	// If there are quotes
	if(!quotes.isEmpty) {
		// Push them out for each pushee
		stockPushees.foreach { stockPushee => 
	    	stockPushee.pushee.push(views.html.poll(quotes).toString.trim) 
	    	Logger.info("Processed pushee: " + stockPushee)
		}
		
		// TODO Send out the Json promises
		// jsonPromises...
	}
    
    // Close the ones that have expired
    // TODO Figure out why this retain is not working
    stockPushees.retain { stockPushee => 
	    stockPushee.count += 1
	    
	    if(stockPushee.count > 3) {
	    	Logger.info("Removing pushee: " + stockPushee)
	    	stockPushee.pushee.close
	    }
	    
      	stockPushee.count <= 3
    }
  }
  
  def view = Action {
    Ok(views.html.view())
  }
}