package controllers

import models.MongoDao
import play.api._

object Global extends GlobalSettings {
  val dao = MongoDao("test")

  override def onStop(app: play.api.Application) = {
    dao.map(_.close())
  }
}