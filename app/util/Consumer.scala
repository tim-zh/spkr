package util

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class Consumer(interval: Int) {
  private var topic: String = _
  private var consumer: KafkaConsumer[String, String] = _
  private var enabled = true
  private val seekRequired = new AtomicBoolean()

  def start(onMessage: Seq[JsValue] => Unit) = Future {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", Random.nextInt() + "")
    props.put("enable.auto.commit", "true")
    props.put("key.deserializer", classOf[StringDeserializer].getName)
    props.put("value.deserializer", classOf[StringDeserializer].getName)
    consumer = new KafkaConsumer[String, String](props)

    while (enabled) {
      try {
        val records = consumer.poll(interval)
        val messages = for (record <- records)
          yield Json.parse(record.value)
        onMessage(messages.toSeq)
      } catch {
        case e: WakeupException =>
          consumer.unsubscribe()
          if (topic != null)
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

  def stop() = {
    topic = null
    enabled = false
    consumer.wakeup()
  }

  def change(id: String) = {
    if (consumer == null)
      throw new IllegalStateException("call start() first")
    topic = id
    seekRequired.set(true)
    consumer.wakeup()
  }
}
