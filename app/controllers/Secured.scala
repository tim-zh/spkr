package controllers

import models.StaticInjections
import play.api.mvc.Security.AuthenticatedBuilder

object Secured extends AuthenticatedBuilder( req =>
  req.session.get("sname").flatMap(StaticInjections.dao.user.get)
)
