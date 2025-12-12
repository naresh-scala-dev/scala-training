package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.typesafe.scalalogging.LazyLogging
import cache.CacheService
import javax.inject._

@Singleton
class HealthController @Inject()(
                                  cc: ControllerComponents,
                                  cacheService: CacheService
                                ) extends AbstractController(cc) with LazyLogging {

  def health() = Action { implicit request =>
    val startTime = System.currentTimeMillis()

    val stats = cacheService.getStats
    val statsJson = convertMapToJson(stats)

    val elapsed = System.currentTimeMillis() - startTime
    logger.info(s"✓ GET /health returned in ${elapsed}ms")

    Ok(Json.obj(
      "status" -> "UP",
      "responseTimeMs" -> elapsed,
      "cache" -> statsJson
    ))
  }

  def cacheStats() = Action { implicit request =>
    val startTime = System.currentTimeMillis()

    val stats = cacheService.getStats
    val statsJson = convertMapToJson(stats)

    val elapsed = System.currentTimeMillis() - startTime
    logger.info(s"✓ GET /cache/stats returned in ${elapsed}ms")

    Ok(Json.obj(
      "stats" -> statsJson,
      "responseTimeMs" -> elapsed
    ))
  }

  /**
   * Convert Map[String, Any] to JsObject
   */
  private def convertMapToJson(map: Map[String, Any]): play.api.libs.json.JsValue = {
    play.api.libs.json.JsObject(
      map.map { case (key, value) =>
        key -> convertToJsValue(value)
      }
    )
  }

  private def convertToJsValue(value: Any): play.api.libs.json.JsValue = value match {
    case m: Map[String, Any] => convertMapToJson(m)
    case s: String => Json.toJson(s)
    case i: Int => Json.toJson(i)
    case l: Long => Json.toJson(l)
    case d: Double => Json.toJson(d)
    case b: Boolean => Json.toJson(b)
    case lst: List[_] => Json.toJson(lst.map(convertToJsValue))
    case Some(v) => convertToJsValue(v)
    case None => play.api.libs.json.JsNull
    case other => Json.toJson(other.toString)
  }
}