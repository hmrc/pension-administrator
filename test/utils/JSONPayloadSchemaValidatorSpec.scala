/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.helper.PSASubscriptionFixture
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsError, JsSuccess}

class JSONPayloadSchemaValidatorSpec extends AnyWordSpec with MockitoSugar with Matchers with BeforeAndAfter {

  private val app = new GuiceApplicationBuilder()
    .overrides(
    )
    .build()

  private lazy val jsonPayloadSchemaValidator: JSONPayloadSchemaValidator = app.injector.instanceOf[JSONPayloadSchemaValidator]
  val psaSchema = "/resources/schemas/psaSubscription.json"
  "validateJson" must {

    "Validate valid payload - register PSA" in {

      val result = jsonPayloadSchemaValidator.validateJsonPayload(psaSchema, PSASubscriptionFixture.registerPSAValidPayload)
      result.isSuccess mustBe true
    }

    "Validate invalid payload - register PSA" in {
      val result = jsonPayloadSchemaValidator.validateJsonPayload(psaSchema, PSASubscriptionFixture.registerPSAInValidPayload)
      result match {
        case JsError(error) =>
          val expectedMessage = "(\"#/definitions/individualDetailType/properties/dateOfBirth\"," +
                                " does not match pattern '^(((19|20)([2468][048]|" +
                                "[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)" +
                                "[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|" +
                                "[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$)"
          ErrorDetailsExtractor.getErrors(error) mustBe expectedMessage
        case JsSuccess(_, _) => fail("should not be successful")
      }
    }
    "Validate invalid payload - register PSA with multiple errors" in {
      val result = jsonPayloadSchemaValidator.validateJsonPayload(psaSchema, PSASubscriptionFixture.registerPSAInValidPayloadWithMultipleErrors)
      result match {
        case JsError(error) =>
          val expectedMessage = "(\"#/definitions/individualDetailType\",Property lastName missing.)" +
            "(\"#/definitions/individualDetailType/properties/dateOfBirth\", " +
            "does not match pattern '^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-]" +
            "(0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|" +
            "(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$)"
          ErrorDetailsExtractor.getErrors(error) mustBe expectedMessage
        case JsSuccess(_, _) => fail("should not be successful")
      }
    }

  }
}