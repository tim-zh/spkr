package models.entities

import java.util.Date

import models.Dao
import org.bson.types.ObjectId
import play.api.libs.json.Json
import util.BasicSpec

class ModelsSpec extends BasicSpec {
  "Message" should "convert itself to json" in {
    val authorId = new ObjectId
    val date = new Date(12345678)
    val dateStr = new models.ModelsModule().defaultDateFormat.format(date)
    val msg = app.injector.instanceOf[Message]
    msg.id = 123
    msg.text = "text1"
    msg.audioId = "aId"
    msg.author = authorId
    msg.date = date
    val user = User("N", "", null)
    user.id = authorId
    app.injector.instanceOf[Dao].user.save(user)

    val result = msg.json

    result shouldBe Json.parse(
      s"""{
        |  "id": ${msg.id},
        |  "author": "N",
        |  "date": "$dateStr",
        |  "text": "${msg.text}",
        |  "audio": "${msg.audioId}"
        |}
      """.stripMargin)
  }
}
