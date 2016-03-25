package common

import java.util.Properties

import util.Producer
import com.google.inject._
import com.mongodb.MongoClient
import models.{Dao, DaoImpl}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

class Dependencies extends AbstractModule {
  val producer = {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("acks", "all")
    props.put("retries", "0")
    props.put("linger.ms", "1")
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    new KafkaProducer[String, String](props) with Producer
  }

  override def configure() = {
    bind(classOf[Dao]).to(classOf[DaoImpl]).asEagerSingleton()

    bind(classOf[Producer]).toInstance(producer)
  }

  @Provides
  @Singleton
  def mongoClient() = new MongoClient("localhost", 27017)
}
