package controllers

import play.api.Play
import play.api.mvc.Security.AuthenticatedBuilder

object Secured extends AuthenticatedBuilder( req =>
  req.session.get("sname").flatMap(Play.current.injector.instanceOf(classOf[models.Dao]).user.get)
)
