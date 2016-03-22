package controllers

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Date

import com.google.inject.Inject
import models.entities.{Audio, Message}
import models._
import org.apache.kafka.clients.producer.ProducerRecord
import play.api.Play
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application extends Controller {
	@Inject
	var dao: Dao = _
	lazy val userDao: UserDao = dao.user
	lazy val chatDao: ChatDao = dao.chat
	@Inject
	var producer: PP = _

	def searchUser(query: String) = Action {
		val users = userDao.list(query)
		val jsUsers = JsArray(users.map(user => JsString(user.name)))
		Ok(jsUsers)
	}

	def addChat() = Secured { request =>
		val participants = request.body.asFormUrlEncoded.flatMap(_.get("participants[]")).getOrElse(Seq())
		val titleMaxLength = 50
		var title = request.body.asFormUrlEncoded.flatMap(_.get("title")).map(_.head).getOrElse("")
		if (title.isEmpty)
			title = if (participants.size == 1)
				participants.head
			else
				participants.mkString(", ")
		if (participants.isEmpty)
			BadRequest(jsonErrors("users" -> "empty"))
		else {
			val userOpts = participants.map(username => (username, userDao.get(username)))
			val opt = userOpts.find { case (name, userOpt) => userOpt.isEmpty }
			if (opt.isDefined && opt.get._2.isEmpty)
				BadRequest(jsonErrors("user" -> ("not found - " + opt.get._1)))
			else {
				val users = userOpts.map(_._2.get).filter(request.user != _) :+ request.user
				if (users.size == 1)
					BadRequest(jsonErrors("users" -> "empty"))
				else {
					chatDao.add(title.take(titleMaxLength), users)
					Ok
				}
			}
		}
	}

	def deleteChat() = Secured { implicit request =>
		Form(single("id" -> nonEmptyText)).bindFromRequest.fold(
			bad =>
				BadRequest(jsonErrors(bad.errors)),
			id => {
				val chatOpt = chatDao.get(id)
				chatOpt.map { chat =>
					request.user.chats -= chat.id
					userDao.save(request.user)
					chat.users -= request.user.id
					if (chat.users.isEmpty)
						chatDao.delete(id)
					else
						chatDao.save(chat)
					Ok
				} getOrElse BadRequest(jsonErrors("chat" -> "not found"))
			}
		)
	}

	def chats() = Secured { request =>
		Ok(JsArray(request.user.chats.flatMap(x => chatDao.get(x)).map { c =>
			Json.obj("title" -> c.title, "id" -> c.id.toString)
		}))
	}

	def chatHistory(id: String) = Secured { request =>
		val chatOpt = chatDao.get(id)
		if (chatOpt.isEmpty)
			BadRequest(jsonErrors("chat" -> "not found"))
		else
			Ok(JsArray(chatOpt.get.orderedHistory.map(m => m.json(userDao.getName(m.author)))))
	}

	def chatHistorySocket() = WebSocket.tryAcceptWithActor[JsValue, JsValue] { request =>
		Future.successful {
			if (getUserFromRequest(request).isDefined)
				Right(out => ChatA.get(out, dao))
			else
				Left(BadRequest(jsonErrors("user" -> "not found")))
		}
	}

	def writeToChat() = Secured(parse.multipartFormData) { implicit request =>
		Form(mapping("chatId" -> nonEmptyText, "msg" -> text(1, 140))(ChatMessage.apply)(ChatMessage.unapply)).bindFromRequest.fold(
			bad =>
				BadRequest(jsonErrors(bad.errors)),
			form => {
				val chatOpt = chatDao.get(form.chatId)
				if (chatOpt.isDefined) {
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
					val message = Play.current.injector.instanceOf[Message]
					message.id = newId
					message.text = form.message
					message.audioId = audioId
					message.author = request.user.id
					message.date = new Date()
					chatOpt.get.history :+= message
					chatDao.save(chatOpt.get)
					producer.send(new ProducerRecord[String, String](form.chatId, message.json(request.user.name).toString))
					Ok
				} else
					BadRequest(jsonErrors("chat" -> "not found"))
			}
		)
	}

	def audio(id: String) = Secured {
		chatDao.getAudio(id) map { audio =>
			val name = audio.id.toString
			Result(
				ResponseHeader(200, Map(
					CONTENT_LENGTH -> audio.data.length.toString,
					CONTENT_DISPOSITION -> ("attachment; filename=\"" + name + "\"")
				)),
				Enumerator.fromStream(new ByteArrayInputStream(audio.data))
			)
		} getOrElse BadRequest(jsonErrors("audio" -> "not found"))
	}
}
