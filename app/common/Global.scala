package common

import play.api.GlobalSettings

object Global extends GlobalSettings {
  override def onStop(app: play.api.Application) = {
    app.injector.instanceOf[util.Producer].close()
  }
}