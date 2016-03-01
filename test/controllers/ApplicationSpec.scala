package controllers

import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BasicSpec

class ApplicationSpec extends BasicSpec {

  "Application" should "search for user" in {
    val controller = app.injector.instanceOf[Application]
    val request = FakeRequest("GET", "/v1/user")

    controller.dao.user.add("a", "a")
    controller.dao.user.add("b", "b")
    controller.dao.user.add("ba", "ba")
    val result = controller.searchUser("b").apply(request)

    contentAsJson(result) shouldBe Json.arr("b", "ba")
  }

  it should "add/list/delete chats" in {
    //add
    val controller = app.injector.instanceOf[Application]
    val title = (1 to 50).map(_ => ".").mkString
    val requestAdd = FakeRequest("POST", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("title" -> (title + "!"), "participants[]" -> "b", "participants[]" -> "c")
    val requestAdd2 = FakeRequest("POST", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("title" -> "", "participants[]" -> "a", "participants[]" -> "b")
    controller.dao.user.add("a", "a")
    controller.dao.user.add("b", "b")
    controller.dao.user.add("c", "c")

    val resultAdd = call(controller.addChat(), requestAdd)
    await(resultAdd)
    val resultAdd2 = call(controller.addChat(), requestAdd2)
    await(resultAdd2)

    status(resultAdd) shouldBe OK
    status(resultAdd2) shouldBe OK

    //list
    val requestList = FakeRequest("GET", "/v1/chat").withSession(("sname", "a"))

    val resultList = controller.chats().apply(requestList)
    val (resultTitles, resultIds) = contentAsJson(resultList).as[JsArray].value.map { x =>
      ((x \ "title").as[String], (x \ "id").as[String])
    }.unzip

    status(resultList) shouldBe OK
    resultTitles.size shouldBe 2
    resultTitles should contain(title)
    resultTitles should contain("a, b")

    //delete
    val requestDelete = FakeRequest("DELETE", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("id" -> resultIds(0))

    val resultDelete = controller.deleteChat().apply(requestDelete)

    status(resultDelete) shouldBe OK

    //check delete
    val requestList2 = FakeRequest("GET", "/v1/chat").withSession(("sname", "a"))

    val resultList2 = controller.chats().apply(requestList2)
    val resultList2Seq = contentAsJson(resultList2).as[JsArray].value.map(x => (x \ "title").as[String])

    status(resultList2) shouldBe OK
    resultList2Seq.size shouldBe 1
    resultList2Seq should contain(resultTitles(1))
  }
}
