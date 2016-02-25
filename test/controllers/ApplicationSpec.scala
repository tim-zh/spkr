package controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BasicSpec

class ApplicationSpec extends BasicSpec {

  "Application" should "search for user" in {
    val controller = injector.instanceOf[Application]
    val request = FakeRequest("GET", "/v1/user")

    controller.dao.user.add("a", "a")
    controller.dao.user.add("b", "b")
    controller.dao.user.add("ba", "ba")
    val result = controller.searchUser("b").apply(request)

    contentAsJson(result) shouldBe Json.arr("b", "ba")
  }

  it should "add new chat" in {
    val controller = injector.instanceOf[Application]
    val title = (1 to 50).map(_ => ".").mkString
    val request = FakeRequest("POST", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("title" -> (title + "!"), "participants[]" -> "b", "participants[]" -> "c")

    controller.dao.user.add("a", "a")
    controller.dao.user.add("b", "b")
    controller.dao.user.add("c", "c")
    val result = call(controller.addChat(), request)

    status(result) shouldBe OK
  }
}
