package models

import play.api.libs.json._
import play.modules.reactivemongo.json._

object UserDao extends Dao[User] {
  override val collectionName: String = "users"

  def get(id: String) =
    findOne(Json.obj("_id" -> id))

  def get(name: String, pass: String) =
    findOne(Json.obj("name" -> name, "pass" -> pass))

  def list(nameQuery: String, limit: Int = 20) =
    find(Json.obj("name" -> Json.obj("$regex" -> ("^" + nameQuery))))

  def add(name: String, pass: String) =
    add(Json.obj("name" -> name, "pass" -> pass))
}
