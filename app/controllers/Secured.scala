package controllers

import play.api.mvc.Security.AuthenticatedBuilder

object Secured extends AuthenticatedBuilder(getUserFromRequest)
