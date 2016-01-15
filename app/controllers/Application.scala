package controllers

import java.util.concurrent.TimeoutException

import models.{ConversationDao, UserDao}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import reactivemongo.core.errors.ReactiveMongoException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class Application extends Controller {
  val userDao = UserDao
  val conversationDao = ConversationDao

  def authenticate() = Action.async { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText)(Login.apply)(Login.unapply)).bindFromRequest.fold(
      bad =>
        Future.successful(BadRequest(jsonErrors(bad.errors))),
      form =>
        userDao.get(form.name).map { user =>
          if (user.isDefined && user.get.pass == form.pass)
            Ok("1").withSession("sname" -> user.get.name)
          else
            BadRequest(jsonErrors("user" -> "not found"))
        }
    )
  }

  def addUser() = Action.async { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText, "pass2" -> nonEmptyText)(Register.apply)(Register.unapply)).bindFromRequest.fold(
      bad =>
        Future.successful(BadRequest(jsonErrors(bad.errors))),
      { form =>
        val errors = form.validate
        if (errors.nonEmpty)
          Future.successful(BadRequest(jsonErrors(errors)))
        else
          userDao.add(form.name, form.pass).flatMap { writeResult =>
            if (writeResult.n != 1)
              Future.successful(BadRequest(jsonErrors("user" -> "not found")))
            userDao.get(form.name).map { user =>
              if (user.isDefined && user.get.pass == form.pass)
                Ok("1").withSession("sname" -> user.get.name)
              else
                BadRequest(jsonErrors("user" -> "not found"))
            }
          } recover {
            case e: ReactiveMongoException =>
              BadRequest(jsonErrors("user" -> e.message))
          }
      }
    )
  }

  def searchUser(query: String) = Action.async {
    userDao.list(query).map { users =>
      val jsUsers = JsArray(users.map(user => JsString(user.name)))
      Ok(jsUsers)
    }
  }

  def addConversation() = Secured { request =>
    val title = request.body.asFormUrlEncoded.flatMap(_.get("title")).getOrElse(Seq("")).head
    val participants = request.body.asFormUrlEncoded.flatMap(_.get("participants[]")).getOrElse(Seq[String]())
    if (participants.isEmpty)
      Ok("0")
    else {
      try {
        val userOpts = participants.map(username => Await.result(userDao.get(username), Duration(10, "s")))
        if (userOpts.exists(_.isEmpty))
          BadRequest(jsonErrors("user" -> "not found"))
        else {
          val users = userOpts.map(_.get.name) :+ request.user
          conversationDao.add(title, users)
          Ok("1")
        }
      } catch {
        case e: TimeoutException =>
          BadRequest(jsonErrors("user" -> e.getMessage))
      }
    }
  }
}
