/*
 * Copyright 2018 HM Revenue & Customs
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

import audit.{AuditService, StubSuccessfulAuditService}
import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.helper.ConnectorBehaviours
import models.SchemeReferenceNumber
import org.scalatest._
import play.api.LoggerLike
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsBoolean
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{StubLogger, WireMockHelper}

class SchemeConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  import SchemeConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  lazy val connector: SchemeConnector = injector.instanceOf[SchemeConnector]
  lazy val appConfig: AppConfig = injector.instanceOf[AppConfig]

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

    connector.checkForAssociation(psaId, srn) map { response =>
      response.right.value shouldBe JsBoolean(true)
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

    connector.checkForAssociation(psaId, srn) map { response =>
      response.left.value shouldBe a[BadRequestException]
    }

  }

}

object SchemeConnectorSpec {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")

  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()
  val checkForAssociationUrl = "/pensions-scheme/is-psa-associated"
  val srn = SchemeReferenceNumber("S0987654321")
  val psaId = PsaId("A7654321")

}