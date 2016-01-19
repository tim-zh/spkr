package models

import models.entities.Conversation
import scala.collection.JavaConversions._

object ConversationDao extends Dao {
  def add(title: String, usernames: Seq[String]) =
    datastore.persist(Conversation(title, usernames, Seq()))
}
