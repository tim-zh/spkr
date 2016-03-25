package controllers

import com.google.inject.Inject
import models._
import play.api.data.Forms._
import play.api.data._
import util.{ControllerUtil, Producer}
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

class CChat @Inject() (dao: Dao, producer: Producer) extends Controller with ControllerUtil {
	val userDao = dao.user
	val chatDao = dao.chat

	def create() = Secured { request =>
		val participants = request.body.asFormUrlEncoded.flatMap(_.get("participants[]")).getOrElse(Seq())
		val titleMaxLength = 50
		var title = request.body.asFormUrlEncoded.flatMap(_.get("title")).map(_.head).getOrElse("")
		if (title.isEmpty)
			title = if (participants.size == 1)
				participants.head
			else
				participants.mkString(", ")
		if (participants.isEmpty)
			badRequestJson("users" -> "empty")
		else {
			val userOpts = participants.map(username => (username, userDao.get(username)))
			val opt = userOpts.find { case (name, userOpt) => userOpt.isEmpty }
			if (opt.isDefined && opt.get._2.isEmpty)
				badRequestJson("user" -> ("not found - " + opt.get._1))
			else {
				val users = userOpts.map(_._2.get).filter(request.user != _) :+ request.user
				if (users.size == 1)
					badRequestJson("users" -> "empty")
				else {
					chatDao.add(title.take(titleMaxLength), users)
					Ok
				}
			}
		}
	}

	def delete() = Secured { implicit request =>
		Form(single("id" -> nonEmptyText)).bindFromRequest.fold(
			bad =>
				badRequestJson(bad.errors),
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
				} getOrElse
						badRequestJson("chat" -> "not found")
			}
		)
	}

	def list() = Secured { request =>
		Ok(JsArray(request.user.chats.flatMap(x => chatDao.get(x)).map { c =>
			Json.obj("title" -> c.title, "id" -> c.id.toString)
		}))
	}
}
