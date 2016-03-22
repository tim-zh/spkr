package controllers

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Collections, Properties}

import akka.actor.ActorRef
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, KafkaConsumer, Consumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json.{JsArray, Json}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class TopicListener(interval: Int) {
  private var topic: String = _
  private val seekRequired = new AtomicBoolean()
  private var consumer: Consumer[String, String] = _
  private var doPoll = true

  def start(messageSink: ActorRef) = Future {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", Random.nextInt() + "")
    props.put("enable.auto.commit", "true")
    props.put("key.deserializer", classOf[StringDeserializer].getName)
    props.put("value.deserializer", classOf[StringDeserializer].getName)
    consumer = new KafkaConsumer[String, String](props)

    while (doPoll) {
      try {
        val records = consumer.poll(interval)
        val messages = for (record <- records)
          yield Json.parse(record.value)
        messageSink ! JsArray(messages.toSeq)
      } catch {
        case e: WakeupException =>
          consumer.unsubscribe()
          consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener {
            override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]) =
              if (seekRequired.compareAndSet(true, false))
                partitions.foreach(consumer.seekToBeginning(_))

            override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]) = {}
          })
      }
    }
    consumer.close()
  }

  def stop() =
    doPoll = false

  def setTopic(id: String) = {
    topic = id
    seekRequired.set(true)
    consumer.wakeup()
  }
}
