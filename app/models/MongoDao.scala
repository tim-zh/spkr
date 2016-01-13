package models

import play.api.Play.current
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import models.User._

object MongoDao {
  private val mongoApi = current.injector.instanceOf[ReactiveMongoApi]

  private def collection(name: String): JSONCollection = mongoApi.db.collection[JSONCollection](name)

  def getUser(id: String) =
    collection("users").find(Json.obj("_id" -> id)).one[User]

  def getUser(name: String, pass: String) =
    collection("users").find(Json.obj("name" -> name, "pass" -> pass)).one[User]

  def addUser(name: String, pass: String) =
    collection("users").insert(Json.obj("name" -> name, "pass" -> pass))
}
