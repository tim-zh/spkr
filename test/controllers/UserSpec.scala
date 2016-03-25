package controllers

import models.Dao
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BasicSpec

class UserSpec extends BasicSpec {
  "CUser" should "search for user" in {
    val controller = app.injector.instanceOf[CUser]
    val dao = app.injector.instanceOf[Dao]
    val request = FakeRequest("GET", "/v1/user")

    dao.user.add("a", "a")
    dao.user.add("b", "b")
    dao.user.add("ba", "ba")
    val result = controller.search("b").apply(request)

    contentAsJson(result) shouldBe Json.arr("b", "ba")
  }

  it should "authenticate a valid user" in {
    val controller = app.injector.instanceOf[CUser]
    val dao = app.injector.instanceOf[Dao]
    val request = userRequest("/v1/authenticate", "nn", "pp")

    dao.user.add("nn", "pp")
    val result = call(controller.authenticate(), request)

    status(result) shouldBe OK
    session(result).apply("sname") shouldBe "nn"
  }

  it should "not authenticate an invalid user" in {
    val controller = app.injector.instanceOf[CUser]
    val dao = app.injector.instanceOf[Dao]
    val request = userRequest("/v1/authenticate", "nn2", "pp2")

    dao.user.add("nn", "pp")
    val result = call(controller.authenticate(), request)

    status(result) shouldBe BAD_REQUEST
    session(result).get("sname") shouldBe None
    dao.user.list("").size shouldBe 1
  }

  it should "register a valid user" in {
    val controller = app.injector.instanceOf[CUser]
    val request = userRequest("/v1/user", "nn", "pp", "pp")

    val result = call(controller.create(), request)

    status(result) shouldBe OK
    session(result).apply("sname") shouldBe "nn"
  }

  it should "not register an invalid user" in {
    val controller = app.injector.instanceOf[CUser]
    val dao = app.injector.instanceOf[Dao]
    val request = userRequest("/v1/user", "n", "pp", "12")

    val result = call(controller.create(), request)

    status(result) shouldBe BAD_REQUEST
    session(result).get("sname") shouldBe None
    contentAsJson(result) shouldBe Json.arr(Json.obj("pass" -> "passwords don't match"))
    dao.user.list("").size shouldBe 0
    //fongo doesn't support indexes
  }

  def userRequest(url: String, name: String, pass: String, pass2: String = null) = {
    if (pass2 == null)
      FakeRequest("POST", url).withJsonBody(Json.obj("name" -> name, "pass" -> pass))
    else
      FakeRequest("POST", url).withJsonBody(Json.obj("name" -> name, "pass" -> pass, "pass2" -> pass2))
  }
}
