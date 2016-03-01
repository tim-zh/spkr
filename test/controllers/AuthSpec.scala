package controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BasicSpec

class AuthSpec extends BasicSpec {
  "Auth" should "authenticate a valid user" in {
    val controller = app.injector.instanceOf[Auth]
    val request = userRequest("/v1/authenticate", "nn", "pp")

    controller.dao.user.add("nn", "pp")
    val result = call(controller.authenticate(), request)

    status(result) shouldBe OK
    session(result).apply("sname") shouldBe "nn"
  }

  it should "not authenticate an invalid user" in {
    val controller = app.injector.instanceOf[Auth]
    val request = userRequest("/v1/authenticate", "nn2", "pp2")

    controller.dao.user.add("nn", "pp")
    val result = call(controller.authenticate(), request)

    status(result) shouldBe BAD_REQUEST
    session(result).get("sname") shouldBe None
    controller.dao.user.list("").size shouldBe 1
  }

  it should "register a valid user" in {
    val controller = app.injector.instanceOf[Auth]
    val request = userRequest("/v1/user", "nn", "pp", "pp")

    val result = call(controller.addUser(), request)

    status(result) shouldBe OK
    session(result).apply("sname") shouldBe "nn"
  }

  it should "not register an invalid user" in {
    val controller = app.injector.instanceOf[Auth]
    val request = userRequest("/v1/user", "n", "pp", "12")

    val result = call(controller.addUser(), request)

    status(result) shouldBe BAD_REQUEST
    session(result).get("sname") shouldBe None
    contentAsJson(result) shouldBe Json.arr(Json.obj("pass" -> "passwords don't match"))
    controller.dao.user.list("").size shouldBe 0
    //fongo doesn't support indexes
  }

  def userRequest(url: String, name: String, pass: String, pass2: String = null) = {
    if (pass2 == null)
      FakeRequest("POST", url).withJsonBody(Json.obj("name" -> name, "pass" -> pass))
    else
      FakeRequest("POST", url).withJsonBody(Json.obj("name" -> name, "pass" -> pass, "pass2" -> pass2))
  }
}
