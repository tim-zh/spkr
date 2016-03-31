package controllers

import akka.actor.{Actor, ActorRef, Props}
import models.Dao
import play.api.libs.json.{Json, JsArray, JsValue}
import util.Consumer

object AChatHistory {
  def get(out: ActorRef, dao: Dao) = Props(new AChatHistory(out, dao))
}

class AChatHistory(out: ActorRef, dao: Dao) extends Actor {
  val consumer = new Consumer(1000)
  consumer.start { messages =>
    out ! JsArray(messages.map(Json.parse))
  }

  override def receive = {
    case msg: JsValue =>
      (msg \ "chatId").asOpt[String].zip((msg \ "lastMsgId").asOpt[Long]).foreach {
        case (chat, offset) => consumer.change(chat, offset)
      }
  }

  override def postStop() =
    consumer.stop()
}
