package controllers

import akka.actor.{Props, Actor, ActorRef}
import models.Dao
import play.api.libs.json.{JsArray, JsValue}

object ChatA {
  def get(out: ActorRef, dao: Dao) = Props(new ChatA(out, dao))
}

class ChatA(out: ActorRef, dao: Dao) extends Actor {
  override def receive = {
    case s: JsValue =>
      val response = (s \ "chatId").asOpt[String].flatMap(dao.chat.get).map { chat =>
        val lastMsgId = (s \ "lastMsgId").asOpt[Long]
        val history = if (lastMsgId.isDefined)
          chat.orderedHistory.dropWhile(_.id <= lastMsgId.get)
        else
          chat.orderedHistory
        JsArray(history.map(_.json))
      } getOrElse {
        jsonErrors("chat" -> "not found")
      }
      out ! response
  }
}
