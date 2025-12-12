package controllers

import play.api.mvc._
import play.api.libs.json.Json
import javax.inject._

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action { implicit request =>
    Ok(Json.obj(
      "message" -> "Play Customer Analytics API",
      "endpoints" -> Json.obj(
        "customer" -> "/customer/:id",
        "summary" -> "/summary/:date/:customerId",
        "summaryAll" -> "/summary/all/:date",
        "events" -> "/events/:customerId/:date",
        "eventsAll" -> "/events/all/:date",
        "health" -> "/health",
        "cache" -> "/cache/stats"
      )
    ))
  }
}