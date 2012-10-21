import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play.current

object Global extends GlobalSettings {
	override def onError(request: RequestHeader, ex: Throwable) = {
		InternalServerError("Error 500")
	}  
	
	override def onHandlerNotFound(request: RequestHeader) = {
		NotFound("Not Found 404")
	}
	
	override def onBadRequest(request: RequestHeader, error: String) = {
		BadRequest("Bad Request 400")
	}  
}