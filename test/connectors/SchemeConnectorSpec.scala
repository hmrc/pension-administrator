/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.helper.ConnectorBehaviours
import models.SchemeReferenceNumber
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues, RecoverMethods}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.*
import repositories.*
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import utils.WireMockHelper

class SchemeConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with MockitoSugar
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  import SchemeConnectorSpec.*

  override protected def portConfigKeys: String = "microservice.services.pensions-scheme.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository])
    )

  lazy val connector: SchemeConnector = injector.instanceOf[SchemeConnector]

  "SchemeConnector checkForAssociation" should "handle OK (200)" in {

    server.stubFor(
      get(urlEqualTo(checkForAssociationUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("psaId", equalTo(psaId.value))
        .withHeader("schemeReferenceNumber", equalTo(srn))
        .willReturn(
          ok(JsBoolean(true).toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.checkForAssociation(Left(psaId), srn) map { response =>
      response.value.shouldBe(true)
    }

  }

  it should "relay BadRequestException when headers are missing" in {

    server.stubFor(
      get(urlEqualTo(checkForAssociationUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          badRequest
            .withBody("Bad Request with missing parameters PSA Id or SRN")
        )
    )

    connector.checkForAssociation(Left(psaId), srn) map { response =>
      response.left.value.shouldBe(a[BadRequestException])
    }

  }

  "SchemeConnector listSchemes" should "handle OK (200)" in {
    server.stubFor(
      get(urlEqualTo(listSchemesUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("idType", equalTo("PSA"))
        .willReturn(
          ok(Json.stringify(validListOfSchemeResponse))
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.listOfSchemes map { response =>
      response.value.shouldBe(validListOfSchemeResponse)
    }
  }

  it should "throw Upstream4xx Exception when DES/ETMP throws BadRequestException" in {
    server.stubFor(
      get(urlEqualTo(listSchemesUrl))
        .willReturn(
          badRequest()
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.listOfSchemes
    }
  }

  it should "return a NotFoundException Exception when DES/ETMP throws NotFoundException" in {
    server.stubFor(
      get(urlEqualTo(listSchemesUrl))
        .willReturn(
          notFound()
        )
    )
    
    connector.listOfSchemes.map { response =>
      response.left.value.shouldBe(a[NotFoundException])
    }
  }
}

object SchemeConnectorSpec extends JsonFileReader {
  private val validListOfSchemeResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val checkForAssociationUrl = "/pensions-scheme/is-psa-associated"
  val listSchemesUrl = "/pensions-scheme/list-of-schemes-self"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("S0987654321")
  val psaId: PsaId = PsaId("A7654321")

}
