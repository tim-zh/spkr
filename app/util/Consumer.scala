package util

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, Future}
import scala.util.Random

class Consumer(interval: Int) {
  private var topic: String = _
  private var offset: Long = -1
  private var consumer: KafkaConsumer[String, String] = _
  private var enabled = true
  private val seekRequired = new AtomicBoolean()

  def start(onMessage: Seq[String] => Unit): Unit = {
    val promise = Promise[Unit]()
    Future {
      val props: Properties = new Properties
      props.put("bootstrap.servers", "localhost:9092")
      props.put("group.id", Random.nextInt() + "")
      props.put("enable.auto.commit", "true")
      props.put("key.deserializer", classOf[StringDeserializer].getName)
      props.put("value.deserializer", classOf[StringDeserializer].getName)
      consumer = new KafkaConsumer[String, String](props)
      promise.success(())

      while (enabled) {
        try {
          val records = consumer.poll(interval)
          val messages = for (record <- records)
            yield record.value
          if (messages.nonEmpty)
            onMessage(messages.toSeq)
        } catch {
          case e: WakeupException =>
            consumer.unsubscribe()
            if (topic != null)
              consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener {
                override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]) =
                  if (seekRequired.compareAndSet(true, false)) {
                    if (offset == -1)
                      consumer.seekToBeginning()
                    else
                      partitions.foreach(consumer.seek(_, offset))
                  }

                override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]) = {}
              })
        }
      }
      consumer.close()
    }
    Await.ready(promise.future, Duration.Inf)
  }

  def change(topic: String, offset: Long = -1) = {
    if (consumer == null)
      throw new IllegalStateException("call start() first")
    this.topic = topic
    this.offset = offset
    seekRequired.set(true)
    consumer.wakeup()
  }

  def stop() = {
    if (consumer == null)
      throw new IllegalStateException("call start() first")
    topic = null
    offset = 0
    enabled = false
    consumer.wakeup()
  }
}
