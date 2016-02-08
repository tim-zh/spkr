package controllers

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Date

import com.google.inject.Inject
import models.entities.{Audio, Message}
import models.{ChatDao, Dao, UserDao}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class Application extends Controller {
	@Inject
	var dao: Dao = _
	lazy val userDao: UserDao = dao.user
	lazy val chatDao: ChatDao = dao.chat

	def searchUser(query: String) = Action {
		val users = userDao.list(query)
		val jsUsers = JsArray(users.map(user => JsString(user.name)))
		Ok(jsUsers)
	}

	def addChat() = Secured { request =>
		val title = request.body.asFormUrlEncoded.flatMap(_.get("title")).getOrElse(Seq("")).head
		val participants = request.body.asFormUrlEncoded.flatMap(_.get("participants[]")).getOrElse(Seq[String]())
		if (participants.isEmpty)
			BadRequest(jsonErrors("users" -> "empty"))
		else {
			val userOpts = participants.map(username => (username, userDao.get(username)))
			val opt = userOpts.find { case (name, userOpt) => userOpt.isEmpty }
			if (opt.isDefined && opt.get._2.isEmpty)
				BadRequest(jsonErrors("user" -> ("not found - " + opt.get._1)))
			else {
				val users = userOpts.map(_._2.get).filter(request.user != _) :+ request.user
				chatDao.add(title, users)
				Ok("")
			}
		}
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
			Ok(JsArray(chatOpt.get.orderedHistory.map(_.json)))
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
					chatOpt.get.history :+= Message(form.message, audioId, request.user.id, new Date())
					chatDao.save(chatOpt.get)
					Ok("")
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
