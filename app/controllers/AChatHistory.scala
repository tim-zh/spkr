package controllers

import java.util.{Collections, Properties}

import akka.actor.{Actor, ActorRef, Props}
import models.Dao
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.Random

object AChatHistory {
  def get(out: ActorRef, dao: Dao) = Props(new AChatHistory(out, dao))
}

class AChatHistory(out: ActorRef, dao: Dao) extends Actor {
  var consumer: Consumer[String, String] = null

  override def receive = {
    case msg: JsValue =>
      (msg \ "chatId").asOpt[String].zip((msg \ "lastMsgId").asOpt[Long]).foreach {
        case (chat, offset) =>
          if (consumer != null)
            consumer.wakeup()
          consumer = listen(chat, { messages =>
            out ! JsArray(messages.map(Json.parse))
          }, offset)
      }
  }

  override def postStop() =
    if (consumer != null)
      consumer.wakeup()

  def listen(topic: String, onMessage: Seq[String] => Unit, offset: Long = -1, interval: Int = 100) = {
    var c: Consumer[String, String] = null
    val consumerCreated = Promise[Unit]()
    Future {
      c = newConsumer()
      c.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener {
        override def onPartitionsAssigned(partitions: java.util.Collection[TopicPartition]) = {
          if (offset == -1)
            c.seekToBeginning()
          else
            partitions.foreach(c.seek(_, offset))
          consumerCreated.success(())
        }

        override def onPartitionsRevoked(partitions: java.util.Collection[TopicPartition]) = {}
      })
      try {
        while (true) {
          val records = c.poll(interval)
          val messages = for (record <- records)
            yield record.value
          if (messages.nonEmpty)
            onMessage(messages.toSeq)
        }
      } finally
        c.close()
    }
    Await.ready(consumerCreated.future, Duration.Inf)
    c
  }

  def newConsumer() = {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", Random.nextInt() + "")
    props.put("enable.auto.commit", "true")
    props.put("session.timeout.ms", "10000")
    props.put("key.deserializer", classOf[StringDeserializer].getName)
    props.put("value.deserializer", classOf[StringDeserializer].getName)
    new KafkaConsumer[String, String](props)
  }
}
