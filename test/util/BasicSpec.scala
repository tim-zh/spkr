package util

import com.github.fakemongo.Fongo
import com.mongodb.{BasicDBObject, MongoClient}
import models.ModelsModule
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, WsScalaTestClient}
import play.api.Application
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{Injector, bind}
import play.api.test.FakeApplication

import scala.collection.JavaConversions._

abstract class BasicSpec extends FlatSpec with Matchers with WsScalaTestClient with OneAppPerSuite with BeforeAndAfter {
  var fongo: Fongo = _
  var injector: Injector = _
  val injectorMerger = InjectorMerger(null, null)

  before {
    fongo = new Fongo("test")
    injector = new GuiceInjectorBuilder().
        bindings(new ModelsModule).
        overrides(bind[MongoClient].toInstance(fongo.getMongo)).
        injector
    injectorMerger.first = injector
  }

  implicit override lazy val app: Application = new FakeApplication() {
    override def injector: Injector = { injectorMerger.second = super.injector; injectorMerger }
  }

  def collection(name: String) = fongo.getDB("test").getCollection(name)

  def dbObject(elems: (AnyRef, AnyRef)*) = new BasicDBObject(mapAsJavaMap(elems.toMap))
}
