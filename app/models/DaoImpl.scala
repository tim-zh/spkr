package models


import com.mongodb.DuplicateKeyException
import models.entities._
import org.bson.types.ObjectId
import scala.collection.JavaConversions._

import scala.reflect._

class DaoImpl extends Dao {
  override val user: UserDao = new UserDaoImpl
  override val chat: ChatDao = new ChatDaoImpl
}

class BaseDaoImpl[T: ClassTag] extends BaseDao[T] {
  override val datastore: DSImpl = DS.datastore

  override def get(id: ObjectId): Option[T] =
    Option(datastore.get(classTag[T].runtimeClass, id).asInstanceOf[T])

  override def save(entity: T) =
    datastore.persist(entity)
}

class UserDaoImpl extends BaseDaoImpl[User] with UserDao {
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

class ChatDaoImpl extends BaseDaoImpl[Chat] with ChatDao {
  override def add(title: String, users: Seq[User]) = {
    val chat = Chat(title, Seq(), users.map(_.id))
    val result = datastore.persist(chat)
    users.foreach { user =>
      user.chats :+= chat.id
      datastore.persist(user)
    }
    result
  }

  override def get(id: String): Option[Chat] =
    Option(datastore.get(classOf[Chat], new ObjectId(id)))
}
