package models

import com.google.inject.Inject
import com.mongodb.{MongoClient, DuplicateKeyException, WriteResult}
import models.entities._
import org.bson.types.ObjectId
import org.mongodb.morphia.Morphia

import scala.collection.JavaConversions._
import scala.reflect._

class DaoImpl @Inject() (mongoClient: MongoClient) extends Dao {
  val datastore: DSImpl = {
    val morphia = new Morphia
    morphia.mapPackage("app.models.entities")
    val result = new DSImpl(morphia, mongoClient, "test")
    result.ensureIndexes()
    result
  }
  override val user: UserDao = new UserDaoImpl(this)
  override val chat: ChatDao = new ChatDaoImpl(this)
}

abstract class BaseDaoImpl[T: ClassTag] extends BaseDao[T] {

  override def get(id: ObjectId): Option[T] =
    Option(datastore.get(classTag[T].runtimeClass, id).asInstanceOf[T])

  override def save(entity: T) =
    datastore.persist(entity)

  override def delete(id: String) = {
    val objectId = getId(id)
    if (objectId.isDefined)
      datastore.delete(classTag[T].runtimeClass, objectId.get)
    else
      new WriteResult(0, false, false)
  }

  protected def getId(id: String) =
    try
      Some(new ObjectId(id))
    catch {
      case e: IllegalArgumentException => None
    }
}

class UserDaoImpl(daoImpl: DaoImpl) extends BaseDaoImpl[User] with UserDao {
  override def datastore: DSImpl = daoImpl.datastore

  override def get(name: String): Option[User] =
    Option(datastore.find(classOf[User], "name", name).get())

  override def list(nameQuery: String, limit: Int): Seq[User] =
    datastore.createQuery(classOf[User]).field("name").startsWithIgnoreCase(nameQuery).limit(limit).asList()

  override def add(name: String, pass: String) =
    try
      Right(datastore.persist(User(name, pass, Seq())))
    catch {
      case e: DuplicateKeyException =>
        Left("user" -> ("duplicate name " + name))
    }
}

class ChatDaoImpl(daoImpl: DaoImpl) extends BaseDaoImpl[Chat] with ChatDao {
  override def datastore: DSImpl = daoImpl.datastore

  override def add(title: String, users: Seq[User]) = {
    val chat = Chat(title, Seq(), users.map(_.id))
    val result = datastore.persist(chat)
    users.foreach { user =>
      user.chats :+= chat.id
      datastore.persist(user)
    }
    result
  }

  private def internalGet[T](id: String, clazz: Class[T]) =
    getId(id).flatMap(objectId => Option(datastore.get(clazz, objectId)))

  override def get(id: String): Option[Chat] =
    internalGet(id, classOf[Chat])

  override def saveAudio(entity: Audio) =
    datastore.persist(entity)

  override def getAudio(id: String) =
    internalGet(id, classOf[Audio])
}
