package info.cmlubinski.loggingexample.lib

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.rest._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

object RestDispatch extends RestHelper {
  serve {
    case req@Req("api" :: _, _, httpMethod) => {
      val startTime = System.currentTimeMillis
      //  Logger may be on a different thread by the time params are processed; force evaluation now
      req.params

      dispatch(req) match {
        case response@Full(json) => {
          RespLogger ! Success(req, json, startTime, System.currentTimeMillis)
          response
        }
        case response@Empty => {
          RespLogger ! FourOhFour(req, startTime, System.currentTimeMillis)
          () => response
        }
        case Failure(msg, _, _) => {
          val response = ("status" -> "failure") ~ ("reason" -> msg)
          RespLogger ! Fail(req, response, startTime, System.currentTimeMillis)
          Full(response)
        }
      }
    }
  }
  def dispatch(req:Req) = {
    try {
      //  Do something complicated
      Full(
        ("status" -> "success") ~
        ("foo" -> "bar")
      )
    } catch {case e => Failure(e.getMessage(), Full(e), Empty)}
  }
}
