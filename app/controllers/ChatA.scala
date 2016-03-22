package controllers

import java.util
import java.util.{Collections, Properties}

import akka.actor.{Actor, ActorRef, Props}
import models.Dao
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, Consumer, ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.collection.JavaConversions._
import scala.util.Random

object ChatA {
  def get(out: ActorRef, dao: Dao) = Props(new ChatA(out, dao))
}

class ChatA(out: ActorRef, dao: Dao) extends Actor {
  object doPoll

  var consumer: Consumer[String, String] = _
  var seekRequired = false

  override def preStart() = {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", Random.nextInt() + "")
    props.put("enable.auto.commit", "true")
    props.put("key.deserializer", classOf[StringDeserializer].getName)
    props.put("value.deserializer", classOf[StringDeserializer].getName)
    consumer = new KafkaConsumer[String, String](props)
    self ! doPoll
  }

  override def receive = {
    case s: JsValue =>
      consumer.unsubscribe()
      consumer.wakeup()
      (s \ "chatId").asOpt[String].foreach { chatId =>
        seekRequired = true
        consumer.subscribe(Collections.singletonList(chatId), new ConsumerRebalanceListener {
          override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]) = {
            if (seekRequired) {
              seekRequired = false
              partitions.foreach(consumer.seekToBeginning(_))
            }
          }

          override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]) = {}
        })
      }

    case doPoll =>
      val records =
        try {
          consumer.poll(1000).iterator() //todo consumer in a dedicated thread
        } catch {
          case e: WakeupException =>
            new java.util.ArrayList[ConsumerRecord[String, String]].iterator
        }
      val messages = for (record <- records)
        yield Json.parse(record.value)
      out ! JsArray(messages.toSeq)
      self ! doPoll
  }

  override def postStop() = {
    consumer.close()
  }
}
