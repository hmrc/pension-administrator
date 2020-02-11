/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http._

// scalastyle:off magic.number

class HttpResponseHelperSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks with MockitoSugar {

  import HttpResponseHelperSpec._

  "handleErrorResponse" should "transform Bad Request into Upstream4xxResponse" in {
    val response = responseFor(BAD_REQUEST)
    a[Upstream4xxResponse] should be thrownBy fixture()(response)
  }

  it should "transform Bad Request into HttpResponse if response contains the value defined" in {
    val response = responseFor(BAD_REQUEST, Some("Bad"))
    response shouldBe a[HttpResponse]
    response.status shouldBe BAD_REQUEST
    response.body shouldBe s"Message for Bad"
  }

  it should "transform Not Found into HttpResponse" in {
    val response = responseFor(NOT_FOUND)
    response shouldBe a[HttpResponse]
    response.status shouldBe NOT_FOUND
  }

  it should "transform any other 4xx into Upstream4xxResponse" in {
    val userErrors = for (n <- Gen.choose(400, 499) suchThat (n => n != 400 && n != 404)) yield n
    forAll(userErrors) {
      userError =>
        val ex = the[Upstream4xxResponse] thrownBy fixture()(responseFor(userError))
        ex.reportAs shouldBe userError
        ex.upstreamResponseCode shouldBe userError
    }
  }

  it should "transform any 5xx into Upstream5xxResponse" in {
    val serverErrors = for (n <- Gen.choose(500, 599)) yield n

    forAll(serverErrors) {
      serverError =>
        val ex = the[Upstream5xxResponse] thrownBy fixture()(responseFor(serverError))
        ex.reportAs shouldBe BAD_GATEWAY
        ex.upstreamResponseCode shouldBe serverError
    }
  }

  it should "transform any other status into an UnrecognisedHttpResponseException" in {
    val statuses = for (n <- Gen.choose(0, 1000) suchThat(n => n < 400 || n >= 600)) yield n

    forAll(statuses) {
      status =>
        an[UnrecognisedHttpResponseException] should be thrownBy fixture()(responseFor(status))
    }
  }

  "parseJson" should "return the correct JsValue given a JSON string" in {
    fixture.parseJson("{}", testMethod, testUrl) shouldBe Json.obj()
  }

  it should "throw BadGatewayException given a non-JSON string" in {

    a[BadGatewayException] should be thrownBy {
      fixture.parseJson("not-json", testMethod, testUrl)
    }

  }

  "validateJson" should "return the correct object given valid JSON" in {

    val expected = Dummy("test-name")
    val json = Json.toJson(expected)

    fixture.validateJson(json, testMethod, testUrl, _ => ()) shouldBe expected

  }

  it should "throw BadGatewayException given invalid JSON" in {

    val json = Json.parse("{}")

    a[BadGatewayException] shouldBe thrownBy {
      fixture.validateJson(json, testMethod, testUrl, _ => ())
    }

  }

  it should "invoke the onInvalid function given invalid JSON" in {

    val json = Json.parse("{}")
    val onInvalid = mock[OnInvalidFunction]

    doNothing().when(onInvalid).onInvalid(any())

    a[BadGatewayException] shouldBe thrownBy {
      fixture.validateJson(json, testMethod, testUrl, onInvalid.onInvalid)
    }

    verify(onInvalid).onInvalid(any())

  }

}

object HttpResponseHelperSpec {

  val testMethod = "test-method"
  val testUrl = "test-url"

  def fixture: HttpResponseHelper = {
    new HttpResponseHelper {}
  }

  def fixture(errorSeq : Seq[String] = Seq()): HttpResponse => HttpException = {
    fixture.handleErrorResponse(testMethod, testUrl, _ ,errorSeq)
  }

  def responseFor(status: Int, body:Option[String]=None): HttpResponse = {
    HttpResponse(status, None, Map.empty, Some(s"Message for ${body.getOrElse(status)}"))
  }

  case class Dummy(name: String)

  implicit val formatsDummy: OFormat[Dummy] = Json.format[Dummy]

  trait OnInvalidFunction {

    def onInvalid(json: JsValue): Unit

  }

}
