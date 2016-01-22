package controllers

import models.{ChatDao, UserDao}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._
import scala.collection.JavaConversions._

class Application extends Controller {
	val userDao = UserDao
	val chatDao = ChatDao

	def authenticate() = Action { implicit request =>
		Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText)(Login.apply)(Login.unapply)).bindFromRequest.fold(
			bad =>
				BadRequest(jsonErrors(bad.errors)),
			form => {
				val user = userDao.get(form.name)
				if (user.isDefined && user.get.pass == form.pass)
					Ok("").withSession("sname" -> user.get.name)
				else
					BadRequest(jsonErrors("user" -> "not found"))
			}
		)
	}

	def addUser() = Action { implicit request =>
		Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText, "pass2" -> nonEmptyText)(Register.apply)(Register.unapply)).bindFromRequest.fold(
			bad =>
				BadRequest(jsonErrors(bad.errors)),
			form => {
				val errors = form.validate
				if (errors.nonEmpty)
					BadRequest(jsonErrors(errors))
				else {
					val result = userDao.add(form.name, form.pass)
					if (result.isLeft)
						BadRequest(jsonErrors(result.left.get))
					else
						Ok("").withSession("sname" -> form.name)
				}
			}
		)
	}

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

	def listChats() = Secured { request =>
		Ok(JsArray(request.user.chats.flatMap(x => chatDao.get(x)).map { c =>
			Json.obj("title" -> c.title, "id" -> c.id.toString)
		}))
	}

	def chatHistory(id: String) = Secured { request =>
		val chatOpt = chatDao.get(id)
		if (chatOpt.isEmpty)
			BadRequest(jsonErrors("chat" -> "not found"))
		else
			Ok(JsArray(chatOpt.get.history map JsString))
	}

	def writeToChat() = Secured { implicit request =>
		Form(mapping("chatId" -> nonEmptyText, "msg" -> text(1, 140))(ChatMessage.apply)(ChatMessage.unapply)).bindFromRequest.fold(
			bad =>
				BadRequest(jsonErrors(bad.errors)),
			form => {
				val chatOpt = chatDao.get(form.chatId)
				if (chatOpt.isDefined) {
					chatOpt.get.history :+= form.message
					chatDao.save(chatOpt.get)
					Ok("")
				} else
					BadRequest(jsonErrors("chat" -> "not found"))
			}
		)
	}
}
