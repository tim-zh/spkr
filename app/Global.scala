import models.PP
import play.api._

object Global extends GlobalSettings {
  override def onStop(app: play.api.Application) = {
    app.injector.instanceOf[PP].close()
  }
}