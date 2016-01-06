package models

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class MongoDao private (dbName: String, servers: Seq[String]) {
  import MongoDao._

  private val driver = new MongoDriver
  private val connection = driver.connection(servers)
  private val db = connection(dbName)

  private def collection(name: String): BSONCollection = db(name)

  def close() = driver.close()

  private def getAll[T](collectionName: String, selector: BSONDocument, projection: BSONDocument)(implicit reader: BSONDocumentReader[T]) =
    if (projection == null)
      collection(collectionName).find(selector).cursor[T]().collect[Seq]()
    else
      collection(collectionName).find(selector, projection).cursor[T]().collect[Seq]()

  def getUsers(selector: BSONDocument, projection: BSONDocument = null) = getAll[User]("users", selector, projection)
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
    override def read(bson: BSONDocument): User = User(bson.getAs[String]("name").get, bson.getAs[String]("pass").get)
  }
}
