package info.cmlubinski.loggingexample.lib

import net.liftweb.common._
import net.liftweb.actor._
import net.liftweb.http._
import net.liftweb.json.JsonAST._
import net.liftweb.mongodb.{MongoDB, DefaultMongoIdentifier, JObjectParser}

sealed abstract class LoggerMsg()
case class Success(req:Req, json:JObject, start:Long, end:Long) extends LoggerMsg
case class FourOhFour(req:Req, start:Long, end:Long) extends LoggerMsg
case class Fail(req:Req, json:JObject, start:Long, end:Long) extends LoggerMsg

object RespLogger extends SpecializedLiftActor[LoggerMsg] {
  override protected def messageHandler = {
    case Success(req, json, start, end) => save(req, Some(json), start, end, 200)
    case FourOhFour(req, start, end) => save(req, None, start, end, 404)
    case Fail(req, json, start, end) => save(req, Some(json), start, end, 500)
  }
  private def save(req:Req, response:Option[JObject], start:Long, end:Long, code:Int) = {
    //  handler: path + method
    val handler = JField("path", JString(req.path.partPath.mkString("/"))) :: 
      JField("method", JString(req.requestType.method)) :: Nil

    //  input: json or query params
    val input = List(inputFields(req))

    //  output: json body, status code
    val output = JField("status_code", JInt(BigInt(code))) ::
      (response match {
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
  private def inputFields(req:Req) = {
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
  def clean[T <: JValue](jvalue:T): T = (jvalue match {
    case JField("password", JString(_)) => JField("password", JString("********"))
    case JField("password", JArray(passwords)) => 
      JField("password", JArray(passwords.map(p => JString("********"))))
    case JField(name, value) => JField(name.replace('.', '_'), clean(value))
    case JObject(fields) => JObject(fields.map(f => clean(f)))
    case JArray(bits) => JArray(bits.map(b => clean(b)))
    case other => other
  }).asInstanceOf[T]
}
