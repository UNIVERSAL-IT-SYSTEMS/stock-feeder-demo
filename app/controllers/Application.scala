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
import play.api.libs.json.Format
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import java.util.UUID


object StockPushee {
  val timeout = 30000L;
}

// Distinguish every request by a unique id
case class StockPushee(val id: UUID) extends Pushee[String] {
  var pushee:Pushee[String] = null
  val timeout = System.currentTimeMillis + StockPushee.timeout
  var complete = false

  def isTimedout = { timeout < System.currentTimeMillis }
  def isComplete = { isTimedout || complete }
  def onComplete = { 
    Logger.info("Pushee complete")
    close
  }
  
  def push(item: String) = {
    pushee.push(item)
  }
  
  def close = {
    pushee.close
    complete = true
    Logger.info("Pushee close")
  }
}

object Application extends Controller {
  var update = true
  
  val stockPushees = new mutable.HashSet[StockPushee]() with mutable.SynchronizedSet[StockPushee]
  val stockJsonPushees = new mutable.HashSet[StockPushee]() with mutable.SynchronizedSet[StockPushee]

  Akka.system.scheduler.schedule(0 seconds, 2 seconds) {
    if(!stockPushees.isEmpty || !stockJsonPushees.isEmpty) {
    	Logger.info("Processing pushees..." + stockPushees + " jsons:" + stockJsonPushees)
    	processPushees
    }
  }
  
  val stockListForm = Form(
		  tuple("stockList" -> nonEmptyText,
		      "update" -> boolean)
  )
    
  def view = Action {
    Ok(views.html.view())
  }
  
  def init = Action {
    Ok(views.html.init(Stock.stockList.values, stockListForm))
  }
  
  def updateInit = Action { implicit request =>
    stockListForm.bindFromRequest.fold(
	  errors => BadRequest(views.html.init(Stock.stockList.values, errors)),
	  value => { // binding success, you get the actual value
		  Stock.stockList.clear
		  var index = 0
		  value._1.split(",").foreach { symbol => 
		    Stock.stockList.put(symbol, new Stock(index, symbol, 0D, "/")) 
		  	index += 1 
		  }
		  
		  update = value._2
	    
		  Redirect(routes.Application.init)
	  } 
	)
  }  
  
  def poll = Action {
    Ok(views.html.poll(getQuotes.toIterator))
  }
  
  def pollStream = Action {
	  val stockPushee = new StockPushee(UUID.randomUUID())
	  
	  val dataStream = Enumerator.pushee[String] (
		  	onStart = { pushee =>
		  	  stockPushee.pushee = pushee
		  	  
		  	  // Add to the list to process
		  	  Application.stockPushees.add(stockPushee)

		  	  // Push everything the first time a client connects
		  	  stockPushee.push(views.html.poll(Stock.stockList.values.toSeq.sortBy(_.symbol).toIterator).toString.trim)
		  	},
		  	onComplete = { stockPushee.onComplete },
		  	onError = { (error, input) => 
		  	  Logger.error("Error with stream: " + error + " input: " + input) 
		  	  stockPushee.onComplete
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
	  val stockPushee = new StockPushee(UUID.randomUUID())
    
	  val dataStream = Enumerator.pushee[String] (
		  	onStart = { pushee =>
		  	  stockPushee.pushee = pushee
		  	  // Add to the list to process
		  	  stockJsonPushees.add(stockPushee)
		  	  Logger.info("New json pushee")
		  	},
		  	onComplete = { stockPushee.onComplete },
		  	onError = { (error, input) => 
		  	  Logger.error("Error with stream: " + error + " input: " + input)
		  	  stockPushee.onComplete
		  	}
	  )

	  Ok.stream(dataStream).as("application/json")
  }
  
  def viewJson = Action {
    Ok(Json.toJson(Stock.stockList.values.toSeq.sortBy(_.symbol))).as("application/json")
  }
  
  def processPushees = {
    val quotes = getQuotes
	// If there are quotes
	if(!quotes.isEmpty) {
		// Push them out for each pushee
		stockPushees.filter(!_.isComplete).foreach { stockPushee => 
		  	// Push only updated quotes to simple (arduino) clients
	    	stockPushee.pushee.push(views.html.poll(quotes.toIterator).toString.trim) 
	    	Logger.info("Processed pushee: " + stockPushee)
		}
		
		// Send out the Json promises
		stockJsonPushees.filter(!_.isComplete).filter { stockPushee =>
		  	// Push full update to json clients
	    	stockPushee.pushee.push(Json.toJson(Stock.stockList.values.toSeq).toString)
	    	Logger.info("Processed json pushee: " + stockPushee)
	    	// Web clients process streams slightly differently, close the connection right after
	    	// new data is pushed to force a reconnect
	    	stockPushee.close
	    	true
		}
	}
    
    // Close the ones that have expired
    stockPushees.retain { stockPushee => 
	    if(stockPushee.isComplete) {
	    	Logger.info("Removing pushee: " + stockPushee)
	    	stockPushee.close
	    }
	    
	    // Retain items that are not complete
	    !stockPushee.isComplete
    }

    stockJsonPushees.retain { stockPushee =>
      if(stockPushee.isComplete) {
    	  	stockPushee.close
    	  	Logger.info("Removing json pushee: " + stockPushee)
      }
      
      // Retain items that are not complete
      !stockPushee.isComplete
    }
  }
  
  def getQuotes = {
    val updatedStocks = StockFeed.getQuotes(Stock.stockList)
    if(update) {
      updatedStocks.foreach(stock => Stock.stockList.put(stock.fullSymbol, stock) )
	}
    
    updatedStocks
  }
}