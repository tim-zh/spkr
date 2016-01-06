package controllers

import play.api._
import play.api.mvc._
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global

class Application extends Controller {
  val dao = Global.dao.get

  def index = Action.async {
    dao.getUsers(BSONDocument()).map { list =>
      Ok(list.size + "")
    }
  }
}
