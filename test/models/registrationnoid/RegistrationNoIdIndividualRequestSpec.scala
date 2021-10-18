/*
 * Copyright 2021 HM Revenue & Customs
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

package models.registrationnoid

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue, Json}
import utils.{InvalidPayloadHandler, InvalidPayloadHandlerImpl}

class RegistrationNoIdIndividualRequestSpec extends AnyFlatSpec with Matchers {

  import RegistrationNoIdIndividualRequestSpec._

  "RegistrationNoIdIndividualRequest.apiWrites" should "transform a request with full address details" in {

    val actual = Json.toJson(fullAddressRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))

    actual shouldEqual expectedFullAddressJson

  }

  it should "produce valid JSON for a full address details request" in {

    val actual = Json.toJson(fullAddressRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))
    val validationFailures = invalidPayloadHandler.getFailures("/resources/schemas/registrationWithoutIdRequest.json")(actual)

    validationFailures shouldBe empty

  }

  it should "transform a request with minimal address details" in {

    val actual = Json.toJson(minimalAddressRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))

    actual shouldEqual expectedMinimalAddressJson

  }

  it should "produce valid JSON for a minimal address details request" in {

    val actual = Json.toJson(minimalAddressRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))
    val validationFailures = invalidPayloadHandler.getFailures("/resources/schemas/registrationWithoutIdRequest.json")(actual)

    validationFailures shouldBe empty

  }

  "RegistrationNoIdIndividualResponse.apiReads" should "transform a success response" in {

    val actual = responseJson.validate[RegisterWithoutIdResponse]

    actual.fold(
      errors => {
        fail(
          "RegistrationNoIdIndividualResponse is not valid",
          JsResultException(errors)
        )
      },
      response => response shouldBe expectedResponse
    )

  }

}

// scalastyle:off magic.number

object RegistrationNoIdIndividualRequestSpec {

  val acknowledgementReference = "test-acknowledgement-reference"

  val fullAddressRequest = RegistrationNoIdIndividualRequest(
    "John",
    "Smith",
    new LocalDate(1990, 4, 3),
    Address(
      "100, Sutton Street",
      "Wokingham",
      Some("Surrey"),
      Some("London"),
      Some("DH1 4EJ"),
      "GB"
    )
  )

  val expectedFullAddressJson: JsValue = Json.parse(
    """
      |{
      |  "regime": "PODA",
      |  "acknowledgementReference": "test-acknowledgement-reference",
      |  "isAnAgent": false,
      |  "isAGroup": false,
      |  "individual": {
      |    "firstName": "John",
      |    "lastName": "Smith",
      |    "dateOfBirth": "1990-04-03"
      |    },
      |  "address": {
      |    "addressLine1": "100, Sutton Street",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "DH1 4EJ",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {}
      |}
    """.stripMargin
  )

  val minimalAddressRequest = RegistrationNoIdIndividualRequest(
    "John",
    "Smith",
    new LocalDate(1990, 4, 3),
    Address(
      "100, Sutton Street",
      "Wokingham",
      None,
      None,
      None,
      "GB"
    )
  )

  val expectedMinimalAddressJson: JsValue = Json.parse(
    """
      |{
      |  "regime": "PODA",
      |  "acknowledgementReference": "test-acknowledgement-reference",
      |  "isAnAgent": false,
      |  "isAGroup": false,
      |  "individual": {
      |    "firstName": "John",
      |    "lastName": "Smith",
      |    "dateOfBirth": "1990-04-03"
      |    },
      |  "address": {
      |    "addressLine1": "100, Sutton Street",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": null,
      |    "addressLine4": null,
      |    "postalCode": null,
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {}
      |}
    """.stripMargin
  )

  val responseJson: JsValue = Json.parse(
    """
      |{
      |  "processingDate": "2001-12-17T09:30:472",
      |  "sapNumber": "1234567890",
      |  "safeId": "XE0001234567890"
      |}
    """.stripMargin
  )

  val expectedResponse = RegisterWithoutIdResponse("XE0001234567890", "1234567890")

  val logger = Logger(classOf[RegistrationNoIdIndividualRequestSpec])

  val invalidPayloadHandler: InvalidPayloadHandler = new InvalidPayloadHandlerImpl(logger)

}
