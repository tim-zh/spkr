package models

import play.api.libs.json._
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.WriteResult
import scala.concurrent.Future

object UserDao extends Dao[User] {
  override val collectionName: String = "users"
  override implicit val reader: Reads[User] = User.UserReads

  def get(name: String) =
    findOne(Json.obj("name" -> name))

  def list(nameQuery: String, limit: Int = 20) =
    find(Json.obj("name" -> Json.obj("$regex" -> ("^" + nameQuery))))

  def add(name: String, pass: String): Future[WriteResult] =
    add(Json.obj("name" -> name, "pass" -> pass))
}
