package util

import com.github.fakemongo.Fongo
import com.mongodb.{BasicDBObject, MongoClient}
import models.ModelsModule
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.play.{OneAppPerSuite, WsScalaTestClient}
import play.api.Application
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{Injector, bind}
import play.api.test.FakeApplication

import scala.collection.JavaConversions

class BasicSpec extends FlatSpec with Matchers with WsScalaTestClient with OneAppPerSuite {
  val fongo = new Fongo("test")
  val injector = new GuiceInjectorBuilder().
      bindings(new ModelsModule).
      overrides(bind[MongoClient].toInstance(fongo.getMongo)).
      injector
  implicit override lazy val app: Application = new FakeApplication() {
    override def injector: Injector = BasicSpec.this.injector //todo
  }

  def collection(name: String) = fongo.getDB("test").getCollection(name)

  def dbObject(elems: (AnyRef, AnyRef)*) = new BasicDBObject(JavaConversions.mapAsJavaMap(elems.toMap))
}
