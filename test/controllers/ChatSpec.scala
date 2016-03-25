package controllers

import models.Dao
import play.api.libs.json.JsArray
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BasicSpec

class ChatSpec extends BasicSpec {
  "CChat" should "add/list/delete chats" in {
    //add
    val controller = app.injector.instanceOf[CChat]
    val dao = app.injector.instanceOf[Dao]
    val title = (1 to 50).map(_ => ".").mkString
    val requestAdd = FakeRequest("POST", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("title" -> (title + "!"), "participants[]" -> "b", "participants[]" -> "c")
    val requestAdd2 = FakeRequest("POST", "/v1/chat").withSession(("sname", "a")).
        withFormUrlEncodedBody("title" -> "", "participants[]" -> "a", "participants[]" -> "b")
    dao.user.add("a", "a")
    dao.user.add("b", "b")
    dao.user.add("c", "c")

    val resultAdd = call(controller.create(), requestAdd)
    await(resultAdd)
    val resultAdd2 = call(controller.create(), requestAdd2)
    await(resultAdd2)

    status(resultAdd) shouldBe OK
    status(resultAdd2) shouldBe OK

    //list
    val requestList = FakeRequest("GET", "/v1/chat").withSession(("sname", "a"))

    val resultList = controller.list().apply(requestList)
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

    val resultDelete = controller.delete().apply(requestDelete)

    status(resultDelete) shouldBe OK

    //check delete
    val requestList2 = FakeRequest("GET", "/v1/chat").withSession(("sname", "a"))

    val resultList2 = controller.list().apply(requestList2)
    val resultList2Seq = contentAsJson(resultList2).as[JsArray].value.map(x => (x \ "title").as[String])

    status(resultList2) shouldBe OK
    resultList2Seq.size shouldBe 1
    resultList2Seq should contain(resultTitles(1))
  }

  it should "reject requests from unauthorized users" in {
    val controller = app.injector.instanceOf[CChat]
    testSecuredEndpoint(controller.create(), "/v1/chat", "POST")
    testSecuredEndpoint(controller.delete(), "/v1/chat", "DELETE")
    testSecuredEndpoint(controller.list(), "/v1/chat", "GET")
  }
}
