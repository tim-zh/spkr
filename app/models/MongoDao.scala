package models

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.bson.{BSONDocument => Doc}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class MongoDao private(dbName: String, servers: Seq[String]) {
  import MongoDao._

  private val driver = new MongoDriver
  private val connection = driver.connection(servers)
  private val db = connection(dbName)

  private def selectorById(id: String) = Doc("_id" -> BSONObjectID(id))

  private def modifier(updates: Doc) = Doc("$set" -> updates)

  private def collection(name: String): BSONCollection = db(name)

  def close() = driver.close()

  private def getAll[T](collectionName: String, selector: Doc, projection: Doc)(implicit reader: BSONDocumentReader[T]) =
    if (projection == null)
      collection(collectionName).find(selector).cursor[T]().collect[Seq]()
    else
      collection(collectionName).find(selector, projection).cursor[T]().collect[Seq]()

  def getUsers(selector: Doc, projection: Doc = null) = getAll[User]("users", selector, projection)

  def getUser(id: String) = collection("users").find(selectorById(id)).one[User]

  def getUser(name: String, pass: String) = collection("users").find(Doc("name" -> name, "pass" -> pass)).one[User]

  def insertUser(name: String, pass: String) =
    collection("users").insert(Doc("name" -> name, "pass" -> pass))

  def updateUser(user: User, name: String = null, pass: String = null) = {
    var fields = Seq[(String, BSONValue)]()
    if (name != null && name != user.name)
      fields :+= "name" -> BSONString(name)
    if (pass != null && pass != user.pass)
      fields :+= "pass" -> BSONString(pass)
    collection("users").update(selectorById(user.id), modifier(Doc(fields)))
  }
}

object MongoDao {
  def apply(dbName: String, servers: Seq[String] = Seq("localhost")) =
    try
      Success(new MongoDao(dbName, servers))
    catch {
      case e: Throwable =>
        Failure(e)
    }

  private implicit val readerForUsers: BSONDocumentReader[User] = new BSONDocumentReader[User] {
    override def read(bson: Doc): User = User(bson.getAs[String]("id").get, bson.getAs[String]("name").get, bson.getAs[String]("pass").get)
  }
}
