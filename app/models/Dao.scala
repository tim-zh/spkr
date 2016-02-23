package models

import java.lang

import com.mongodb.WriteResult
import models.entities._
import org.bson.types.ObjectId
import org.mongodb.morphia.Key

trait Dao {
  val user: UserDao

  val chat: ChatDao
}

trait BaseDao[T] {
  def datastore: DSImpl

  def get(id: ObjectId): Option[T]

  def save(entity: T): lang.Iterable[Key[T]]

  def delete(id: String): WriteResult
}

trait UserDao extends BaseDao[User] {
  def get(name: String): Option[User]

  def list(nameQuery: String, limit: Int = 20): Seq[User]

  def add(name: String, pass: String): Either[(String, String), lang.Iterable[Key[User]]]
}

trait ChatDao extends BaseDao[Chat] {
  def add(title: String, users: Seq[User]): lang.Iterable[Key[Chat]]

  def get(id: String): Option[Chat]

  def saveAudio(entity: Audio): lang.Iterable[Key[Audio]]

  def getAudio(id: String): Option[Audio]
}