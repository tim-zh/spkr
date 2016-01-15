package models

import play.api.libs.json.{Json, Reads}
import reactivemongo.api.commands.WriteResult
import scala.concurrent.Future

object ConversationDao extends Dao[Conversation] {
  override val collectionName: String = "conversations"
  override implicit val reader: Reads[Conversation] = Conversation.ConversationReads

  def add(title: String, usernames: Seq[String]): Future[WriteResult] =
    add(Json.obj("title" -> title, "users" -> usernames, "history" -> Seq[String]()))
}
