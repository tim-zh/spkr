package controllers

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Date

import com.google.inject.Inject
import models._
import models.entities.{Audio, Message}
import org.apache.kafka.clients.producer.ProducerRecord
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Enumerator
import util.{ControllerUtil, Producer}
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CChatHistory @Inject() (dao: Dao, producer: Producer) extends Controller with ControllerUtil {
  val userDao = dao.user
  val chatDao = dao.chat

  def read(id: String) = Secured { request =>
    val chatOpt = chatDao.get(id)
    if (chatOpt.isEmpty)
      badRequestJson("chat" -> "not found")
    else
      Ok(JsArray(chatOpt.get.orderedHistory.map(m => m.json(userDao.getName(m.author)))))
  }

  def socket() = WebSocket.tryAcceptWithActor[JsValue, JsValue] { request =>
    Future.successful {
      if (getUserFromRequest(request).isDefined)
        Right(out => AChatHistory.get(out, dao))
      else
        Left(badRequestJson("user" -> "not found"))
    }
  }

  def update() = Secured(parse.multipartFormData) { implicit request =>
    case class Data(chatId: String, message: String)

    Form(mapping(
      "chatId" -> nonEmptyText,
      "msg" -> text(1, 140)
    )(Data.apply)(Data.unapply)).bindFromRequest.fold(
      bad =>
        badRequestJson(bad.errors),
      form => {
        val chatOpt = chatDao.get(form.chatId)
        if (chatOpt.isEmpty)
          badRequestJson("chat" -> "not found")
        else {
          val audioId = request.body.file("record").map { file =>
            val byteArray = Files.readAllBytes(file.ref.file.toPath)
            val audio = Audio(byteArray)
            chatDao.saveAudio(audio)
            audio.id.toString
          } getOrElse ""
          val history = chatOpt.get.orderedHistory
          val newId =
            if (history.nonEmpty)
              history.get(history.size - 1).id + 1
            else
              0
          val message = Message(newId, form.message, audioId, request.user.id, new Date)
          chatOpt.get.history :+= message
          chatDao.save(chatOpt.get)
          producer.send(new ProducerRecord[String, String](form.chatId, message.json(request.user.name).toString))
          Ok
        }
      }
    )
  }

  def readAudio(id: String) = Secured {
    chatDao.getAudio(id) map { audio =>
      val name = audio.id.toString
      Result(
        ResponseHeader(200, Map(
          CONTENT_LENGTH -> audio.data.length.toString,
          CONTENT_DISPOSITION -> ("attachment; filename=\"" + name + "\"")
        )),
        Enumerator.fromStream(new ByteArrayInputStream(audio.data))
      )
    } getOrElse
        badRequestJson("audio" -> "not found")
  }
}
