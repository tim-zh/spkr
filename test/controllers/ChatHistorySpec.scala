package controllers

import java.util.Date

import util.BasicSpec
import models.Dao
import models.entities.Message
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ChatHistorySpec extends BasicSpec {
  "CChatHistory" should "read chat history" in {
    val controller = app.injector.instanceOf[CChatHistory]
    val dao = app.injector.instanceOf[Dao]
    //add user
    dao.user.add("a", "a")
    val user = dao.user.get("a").get
    //add chat
    dao.chat.add("title", Seq(user))
    val chat = dao.chat.get(dao.user.get("a").get.chats.get(0)).get
    //add history
    val message1 = Message(0, "111", "", user.id, new Date(100))
    val message2 = Message(1, "222", "", user.id, new Date(101))
    chat.history.add(message2)
    chat.history.add(message1)
    dao.chat.save(chat)
    val request = FakeRequest("GET", "/v1/chat/history").withSession(("sname", "a"))

    val result = controller.read(chat.id.toString).apply(request)

    status(result) shouldBe OK
    contentAsJson(result) shouldBe Json.parse(
      s"""
        |[ ${message1.json(user.name)},
        |  ${message2.json(user.name)} ]
      """.stripMargin)
  }

  it should "add messages to chat history" in {
    val controller = app.injector.instanceOf[CChatHistory]
    val dao = app.injector.instanceOf[Dao]
    //add user
    dao.user.add("a", "a")
    val user = dao.user.get("a").get
    //add chat
    dao.chat.add("title", Seq(user))
    val chat = dao.chat.get(dao.user.get("a").get.chats.get(0)).get
    //add history
    val message = Message(12, "111", "", user.id, new Date(100))
    chat.history.add(message)
    dao.chat.save(chat)
    val text = "123"
    val requestAdd = FakeRequest("POST", "/v1/chat/history").withSession(("sname", "a")).
        withMultipartFormDataBody(MultipartFormData(Map("chatId" -> Seq(chat.id.toString), "msg" -> Seq(text)), Seq(), Seq(), Seq()))
    val requestRead = FakeRequest("GET", "/v1/chat/history").withSession(("sname", "a"))

    val resultAdd = call(controller.update(), requestAdd)
    await(resultAdd)
    val result = call(controller.read(chat.id.toString), requestRead)
    await(result)

    status(resultAdd) shouldBe OK
    status(result) shouldBe OK
    contentAsJson(result).as[JsArray].value.size shouldBe 2
    contentAsJson(result).as[JsArray].value(0) shouldBe message.json(user.name)
    val newMsg = contentAsJson(result).as[JsArray].value(1)
    (newMsg \ "id").as[Int] shouldBe(message.id + 1)
    (newMsg \ "author").as[String] shouldBe user.name.toString
    (newMsg \ "text").as[String] shouldBe text
  }

  it should "reject requests for missing chat" in {
    val controller = app.injector.instanceOf[CChatHistory]
    val dao = app.injector.instanceOf[Dao]
    //add user
    dao.user.add("a", "a")
    val user = dao.user.get("a").get
    //add chat
    dao.chat.add("title", Seq(user))
    val chat = dao.chat.get(dao.user.get("a").get.chats.get(0)).get
    val request = FakeRequest("GET", "/v1/chat/history").withSession(("sname", "a"))

    val result = controller.read("123").apply(request)

    status(result) shouldBe BAD_REQUEST
    contentAsJson(result) shouldBe Json.parse("""[{ "chat": "not found" }]""")
  }

  it should "reject requests from unauthorized users" in {
    val controller = app.injector.instanceOf[CChatHistory]
    testSecuredEndpoint(controller.read(""), "/v1/chat/history")
  }
}
