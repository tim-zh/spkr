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
        JsArray(chat.orderedHistory.map(_.json))
      } getOrElse {
        jsonErrors("chat" -> "not found")
      }
      out ! response
  }
}
