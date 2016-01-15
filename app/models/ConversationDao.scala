package models

import play.api.libs.json._
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.WriteResult
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ConversationDao extends Dao[Conversation] {
  override val collectionName: String = "conversations"
  override implicit val reader: Reads[Conversation] = Conversation.ConversationReads

  def add(title: String, usernames: Seq[String]): Future[WriteResult] =
    add(Json.obj("title" -> title, "users" -> usernames, "history" -> Seq[String]()))

  def listIds(username: String) =
    collection.find(Json.obj("users" -> Json.obj("$in" -> Json.arr(username))), Json.obj("_id" -> "1")).
        cursor[String]().collect[Seq](upTo = 20)
}
