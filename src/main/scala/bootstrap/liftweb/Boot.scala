package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import net.liftweb.mongodb._
import com.mongodb.DB.WriteConcern
import info.cmlubinski.loggingexample.lib._

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    //  MongoDB Setup
    MongoDB.defineDb(DefaultMongoIdentifier, MongoAddress(MongoHost("127.0.0.1", 27017), "loggingexample"))
    MongoDB.use(DefaultMongoIdentifier)(db => db.setWriteConcern(WriteConcern.STRICT))

    LiftRules.statelessDispatchTable.append(RestDispatch)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}
