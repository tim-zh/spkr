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
    val dateStr = Message.dateFormat.format(date)
    val msg = Message(123, "text1", "aId", authorId, date)
    val user = User("N", "", null)
    user.id = authorId
    app.injector.instanceOf[Dao].user.save(user)

    val result = msg.json(app.injector.instanceOf[Dao].user.getName(authorId))

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
