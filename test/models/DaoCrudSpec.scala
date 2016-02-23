package models

import models.entities.User
import org.bson.types.ObjectId
import util.BasicSpec

class DaoCrudSpec extends BasicSpec {
	"User dao" should "consistently perform crud" in {
		val dao = app.injector.instanceOf[Dao].user

		dao.add("name1", "pass1")
		val result = dao.get("name1").get

		result.pass shouldBe "pass1"

		dao.delete(result.id.toString)

		dao.get("name1") shouldBe None
	}

	"Chat dao" should "consistently perform crud" in {
		val dao = app.injector.instanceOf[Dao].chat
		val userDao = app.injector.instanceOf[Dao].user
		val user = User("n", "p", new java.util.ArrayList())
		user.id = new ObjectId

		val chatId = dao.add("title1", Seq(user)).iterator().next().getId
		val result = dao.get(chatId + "").get
		val userResult = userDao.get("n").get

		result.title shouldBe "title1"
		result.users shouldBe java.util.Arrays.asList(user.id)
		userResult.chats shouldBe java.util.Arrays.asList(result.id)

		dao.delete(result.id.toString)

		dao.get("title1") shouldBe None
	}
}
