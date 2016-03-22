package controllers

import akka.actor.{Actor, ActorRef, Props}
import models.Dao
import play.api.libs.json.JsValue

object ChatA {
  def get(out: ActorRef, dao: Dao) = Props(new ChatA(out, dao))
}

class ChatA(out: ActorRef, dao: Dao) extends Actor {
  val consumer = new TopicListener(1000)

  override def preStart() =
    consumer.start(out)

  override def receive = {
    case s: JsValue =>
      (s \ "chatId").asOpt[String].foreach(consumer.setTopic)
  }

  override def postStop() =
    consumer.stop()
}
