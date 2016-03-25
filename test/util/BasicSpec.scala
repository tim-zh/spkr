package util

import java.io.ByteArrayOutputStream

import com.github.fakemongo.Fongo
import com.mongodb.{BasicDBObject, MongoClient}
import common.Dependencies
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest._
import org.scalatestplus.play.{OneAppPerTest, WsScalaTestClient}
import play.api.Application
import play.api.http.Writeable
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class BasicSpec extends FlatSpec with Matchers with WsScalaTestClient with OneAppPerTest with BeforeAndAfter {
  override def newAppForTest(testData: TestData): Application = {
    val fongo = new Fongo("test")
    val newInjector = new GuiceInjectorBuilder().
        bindings(new Dependencies).
        overrides(bind[MongoClient].toInstance(fongo.getMongo)).
        overrides(bind[Producer].toInstance(new MockProducer[String, String](true, new StringSerializer, new StringSerializer) with Producer)).
        injector
    new FakeApplication() {
      private val inj = InjectorMerger(newInjector, super.injector)
      override def injector: Injector = inj
    }
  }

  def dbObject(elems: (AnyRef, AnyRef)*) = new BasicDBObject(mapAsJavaMap(elems.toMap))

  def testSecuredEndpoint[T](endpoint: Action[AnyContent], url: String, method: String = "GET") = {
    val request = FakeRequest(method, url)

    val result = call(endpoint, request)

    status(result) shouldBe UNAUTHORIZED
  }

  implicit val w: Writeable[AnyContentAsMultipartFormData] = { //omg, play, y u no have it out of the box?
    val builder = MultipartEntityBuilder.create().setBoundary("fgsfds")

    def transform(a: AnyContentAsMultipartFormData) = {
      a.mdf.dataParts.foreach {
        case (name, seq) => seq.foreach { value =>
          builder.addPart(name, new StringBody(value, ContentType.create("text/plain", "UTF-8")))
        }
      }
      a.mdf.files.foreach { file =>
        val part = new FileBody(file.ref.file, ContentType.create(file.contentType.getOrElse("application/octet-stream")), file.filename)
        builder.addPart(file.key, part)
      }
      val result = new ByteArrayOutputStream()
      builder.build().writeTo(result)
      result.toByteArray
    }

    Writeable(transform, Some(builder.build.getContentType.getValue))
  }
}
