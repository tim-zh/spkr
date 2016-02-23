package util

import com.github.fakemongo.Fongo
import com.mongodb.{BasicDBObject, MongoClient}
import models.ModelsModule
import org.scalatest._
import org.scalatestplus.play.{OneAppPerTest, WsScalaTestClient}
import play.api.Application
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{Injector, bind}
import play.api.test.FakeApplication

import scala.collection.JavaConversions._

abstract class BasicSpec extends FlatSpec with Matchers with WsScalaTestClient with OneAppPerTest with BeforeAndAfter {
  var injector: Injector = _

  override def newAppForTest(testData: TestData): Application = {
    val fongo = new Fongo("test")
    injector = new GuiceInjectorBuilder().
        bindings(new ModelsModule).
        overrides(bind[MongoClient].toInstance(fongo.getMongo)).
        injector
    new FakeApplication() {
      override def injector: Injector = InjectorMerger(BasicSpec.this.injector, super.injector)
    }
  }

  def dbObject(elems: (AnyRef, AnyRef)*) = new BasicDBObject(mapAsJavaMap(elems.toMap))
}
