package info.cmlubinski.loggingexample.lib

import net.liftweb.common._
import net.liftweb.actor._
import net.liftweb.http._
import net.liftweb.json.JsonAST._
import net.liftweb.mongodb.{MongoDB, DefaultMongoIdentifier, JObjectParser}

class RespLogger(req:Req, start:Long) extends SpecializedLiftActor[RespLogger.LoggerMsg] {
  override protected def messageHandler = {
    case RespLogger.End(json, time) => save(Some(json), time, 200)
    case RespLogger.FourOhFour(time) => save(None, time, 404)
    case RespLogger.Failure(json, time) => save(Some(json), time, 500)
  }
  private def save(json:Option[JObject], end:Long, code:Int) = {
    //  handler: path + method
    val handler = JField("path", JString(req.path.partPath.mkString("/"))) :: 
      JField("method", JString(req.requestType.method)) :: Nil

    //  input: json or query params
    val input = List(inputFields)

    //  output: json body, status code
    val output = JField("status_code", JInt(BigInt(code))) ::
      (json match {
        case Some(jobj) => List(JField("output", jobj))
        case None => Nil
      })
      
    //  metrics: duration, logged at
    val metrics = JField("duration", JInt(BigInt(end - start))) :: 
      JField("logged_at", JInt(BigInt(System.currentTimeMillis))) :: Nil

    //  client: ip addy, user
    val client = JField("ip", JString(req.remoteAddr)) :: magicToFindUserByHeader() :: Nil

    MongoDB.use(DefaultMongoIdentifier) ( db =>
      db.getCollection("resplog").save(JObjectParser.parse(clean(
        JObject(handler ::: input ::: output ::: metrics ::: client)
      ))(net.liftweb.json.DefaultFormats.lossless))
    )
  }
  private def magicToFindUserByHeader() = JField("user_id", JInt(BigInt(1)))
  private def inputFields() = {
    val jsonInput = if (req.json_?) {
      req.json match {
        case Full(body) => List(JField("json_body", body))
        case _ => Nil
      } 
    } else Nil

    var params:List[JField] = Nil
    req.params.foreach(pair => {
      val (field, values) = pair
      params = JField(field, JArray(values.map(v => JString(v)))) :: params
    })

    JField("input", JObject(JField("params", JObject(params)) :: jsonInput))
  }
  def clean[T](jvalue:T): T = jvalue match {
    case JField("password", JString(_)) => JField("password", JString("********")).asInstanceOf[T]
    case JField("password", JArray(passwords)) => 
      JField("password", JArray(passwords.map(p => JString("********")))).asInstanceOf[T]
    case JField(name, value) => JField(name.replace('.', '_'), clean(value)).asInstanceOf[T]
    case JObject(fields) => JObject(fields.map(f => clean(f))).asInstanceOf[T]
    case JArray(bits) => JArray(bits.map(b => clean(b))).asInstanceOf[T]
    case other => other
  }

}

object RespLogger {
  sealed abstract class LoggerMsg()
  case class End(json:JObject, time:Long) extends LoggerMsg
  case class FourOhFour(time:Long) extends LoggerMsg
  case class Failure(json:JObject, time:Long) extends LoggerMsg
}
