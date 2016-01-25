package models

import java.lang

import models.entities.{Chat, User}
import org.bson.types.ObjectId
import org.mongodb.morphia.Key

trait Dao {
  val user: UserDao

  val chat: ChatDao
}

trait BaseDao[T] {
  val datastore: DSImpl

  def get(id: ObjectId): Option[T]

  def save(entity: T): lang.Iterable[Key[T]]
}

trait UserDao extends BaseDao[User] {
  def get(name: String): Option[User]

  def list(nameQuery: String, limit: Int = 20): Seq[User]

  def add(name: String, pass: String): Either[(String, String), lang.Iterable[Key[User]]]
}

trait ChatDao extends BaseDao[Chat] {
  def add(title: String, users: Seq[User]): lang.Iterable[Key[Chat]]

  def get(id: String): Option[Chat]
}