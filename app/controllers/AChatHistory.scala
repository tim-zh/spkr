package controllers

import akka.actor.{Actor, ActorRef, Props}
import models.Dao
import play.api.libs.json.{JsArray, JsValue}
import util.Consumer

object AChatHistory {
  def get(out: ActorRef, dao: Dao) = Props(new AChatHistory(out, dao))
}

class AChatHistory(out: ActorRef, dao: Dao) extends Actor {
  val consumer = new Consumer(1000)

  override def preStart() =
    consumer.start { messages =>
      out ! JsArray(messages)
    }

  override def receive = {
    case msg: JsValue =>
      (msg \ "chatId").asOpt[String].foreach(consumer.change)
  }

  override def postStop() =
    consumer.stop()
}
