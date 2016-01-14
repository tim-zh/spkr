package models

import play.api.Play.current
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import scala.concurrent.ExecutionContext.Implicits.global

trait Dao[T] {
  private val mongoApi = current.injector.instanceOf[ReactiveMongoApi]

  protected def collection: JSONCollection = mongoApi.db.collection[JSONCollection](collectionName)

  val collectionName: String

  def findOne(selector: JsObject) =
    collection.find(selector).one[T]

  def find(selector: JsObject, limit: Int = 20) =
    collection.find(selector).cursor[T]().collect[Seq](upTo = limit)

  def add(x: JsObject) =
    collection.insert(x)
}
