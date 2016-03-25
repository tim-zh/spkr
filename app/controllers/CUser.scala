package controllers

import com.google.inject.Inject
import models.Dao
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc.{Action, Controller}
import util.ControllerUtil

class CUser @Inject()(dao: Dao) extends Controller with ControllerUtil {
  val userDao = dao.user

  def search(query: String) = Action {
    val users = userDao.list(query)
    val jsUsers = JsArray(users.map(user => JsString(user.name)))
    Ok(jsUsers)
  }

  def authenticate() = Action { implicit request =>
    case class Data(name: String, pass: String)

    Form(mapping(
      "name" -> nonEmptyText,
      "pass" -> nonEmptyText
    )(Data.apply)(Data.unapply)).bindFromRequest.fold(
      bad =>
        badRequestJson(bad.errors),
      form =>
        userDao.get(form.name) filter {
          _.pass == form.pass
        } map { user =>
          Ok.withSession("sname" -> user.name)
        } getOrElse
            badRequestJson("user" -> "not found")
    )
  }

  def create() = Action { implicit request =>
    case class Data(name: String, pass: String, pass2: String)

    Form(mapping(
      "name" -> nonEmptyText,
      "pass" -> nonEmptyText,
      "pass2" -> nonEmptyText
    )(Data.apply)(Data.unapply)).bindFromRequest.fold(
      bad =>
        badRequestJson(bad.errors),
      form =>
        if (form.pass != form.pass2)
          badRequestJson("pass" -> "passwords don't match")
        else {
          val result = userDao.add(form.name, form.pass)
          if (result.isLeft)
            badRequestJson(result.left.get)
          else
            Ok.withSession("sname" -> form.name)
        }
    )
  }
}
