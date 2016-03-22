package models

import java.text.SimpleDateFormat
import java.util.Properties

import com.google.inject._
import com.google.inject.name.Names
import com.mongodb.MongoClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.mongodb.morphia.Morphia

class ModelsModule extends AbstractModule {
  val defaultDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss")
  val producer = {
    val props: Properties = new Properties
    props.put("bootstrap.servers", "localhost:9092")
    props.put("acks", "all")
    props.put("retries", "0")
    props.put("linger.ms", "1")
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    new KafkaProducer[String, String](props) with PP
  }

  override def configure() = {
    bind(classOf[Dao]).to(classOf[DaoImpl]).asEagerSingleton()

    bind(classOf[SimpleDateFormat]).annotatedWith(Names.named("default")).toInstance(defaultDateFormat)

    bind(classOf[PP]).toInstance(producer)
  }

  @Provides
  @Singleton
  def createMongoClient(): MongoClient = new MongoClient("localhost", 27017)

  @Provides
  @Singleton
  def createDSImpl(injector: Injector): DSImpl = {
    val morphia = new Morphia
    morphia.getMapper.getOptions.setObjectFactory(new MorphiaCreatorWithInjections)
    morphia.mapPackage("app.models.entities")
    val mongoClient = injector.getInstance(classOf[MongoClient])
    val result = new DSImpl(morphia, mongoClient, "test")
    result.ensureIndexes()
    result
  }
}
