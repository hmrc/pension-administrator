/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import akka.stream.Materializer
import base.SpecBase
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.MongoConnector

class MongoDiagnosticsControllerSpec extends SpecBase with MockitoSugar {

  import MongoDiagnosticsControllerSpec._

  "mongoDiagnostics" should {
    // Difficult to see how to test this - come back to it later..
//    "work" in {
//      val result = controller.mongoDiagnostics()(FakeRequest())
//      status(result) mustBe OK
//    }
  }

}

object MongoDiagnosticsControllerSpec extends MockitoSugar {
  private val app = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build()
  implicit lazy val mat: Materializer = app.materializer
  private val cc = app.injector.instanceOf[ControllerComponents]

  private def configuration = Configuration("mongodb.pension-administrator-cache.maxSize" -> 512000)

  private val mockMongoConnector = mock[MongoConnector]

  //private val db =

  //when(mockMongoConnector.db()).thenReturn(

  private val rmc = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mockMongoConnector
  }
  private val controller = new MongoDiagnosticsController(configuration, rmc, cc)
}



