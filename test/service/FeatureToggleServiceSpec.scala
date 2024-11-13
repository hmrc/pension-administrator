/*
 * Copyright 2024 HM Revenue & Customs
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

package service

import base.SpecBase
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.{EnrolmentRecovery, PsaFromIvToPdv, PsaRegistration, UpdateClientReference}
import models.{FeatureToggle, FeatureToggleName, ToggleDetails}
import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import repositories._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FeatureToggleServiceSpec
  extends SpecBase
    with MockitoSugar
    with ScalaFutures
    with Matchers {

  val adminDataRepository: AdminDataRepository = mock[AdminDataRepository]
  val toggleDataRepository: ToggleDataRepository = mock[ToggleDataRepository]
  val OUT = app.injector.instanceOf[FeatureToggleService]

  private val toggleDetails1 = ToggleDetails("Toggle-name1", Some("Toggle description1"), isEnabled = true)
  private val toggleDetails2 = ToggleDetails("Toggle-name2", Some("Toggle description2"), isEnabled = false)
  private val toggleDetails3 = ToggleDetails("Toggle-name3", Some("Toggle description3"), isEnabled = true)
  private val toggleDetails4 = ToggleDetails("Toggle-name4", Some("Toggle description4"), isEnabled = false)

  private val seqToggleDetails = Seq(toggleDetails1, toggleDetails2, toggleDetails3, toggleDetails4)

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AdminDataRepository].toInstance(adminDataRepository),
      bind[ToggleDataRepository].toInstance(toggleDataRepository),
      bind[AsyncCacheApi].toInstance(new FakeCache())
    )

  implicit private val arbitraryFeatureToggleName: Arbitrary[FeatureToggleName] =
    Arbitrary {
      Gen.oneOf(FeatureToggleName.toggles)
    }

  // A cache that doesn't cache
  class FakeCache extends AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)
                                   (orElse: => Future[A])
                                   (implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)
                       (implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  "When set works in the repo returns a success result" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(adminDataRepository.setFeatureToggles(any())).thenReturn(Future.successful(()))

    val toggleName = arbitrary[FeatureToggleName].sample.value

    whenReady(OUT.set(toggleName = toggleName, enabled = true)) {
      result =>
        result mustBe()
        val captor = ArgumentCaptor.forClass(classOf[Seq[FeatureToggle]])
        verify(adminDataRepository, times(1)).setFeatureToggles(captor.capture())
        captor.getValue must contain(Enabled(toggleName))
    }
  }

  "When getAll is called returns all of the toggles from the repo" in {
    OUT.getAll.futureValue mustBe Seq(
      Disabled(UpdateClientReference),
      Disabled(PsaFromIvToPdv),
      Disabled(PsaRegistration),
      Disabled(EnrolmentRecovery)
    )
  }

  "When a toggle doesn't exist in the repo, return default" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))
    OUT.get(UpdateClientReference).futureValue mustBe Disabled(UpdateClientReference)
  }

  "When a toggle exists in the repo, override default" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq(Enabled(UpdateClientReference))))
    OUT.get(UpdateClientReference).futureValue mustBe Enabled(UpdateClientReference)
  }

  "When upsertFeatureToggle works in the repo, it returns a success result for the toggle data" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(toggleDataRepository.upsertFeatureToggle(any())).thenReturn(Future.successful(()))

    whenReady(OUT.upsertFeatureToggle(toggleDetails1)) {
      result =>
        result mustBe()
        val captor = ArgumentCaptor.forClass(classOf[ToggleDetails])
        verify(toggleDataRepository, times(1)).upsertFeatureToggle(captor.capture())
        captor.getValue mustBe toggleDetails1
    }
  }

  "When deleteToggle works in the repo, it returns a success result for the toggle data" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(toggleDataRepository.upsertFeatureToggle(any())).thenReturn(Future.successful(()))
    when(toggleDataRepository.deleteFeatureToggle(any())).thenReturn(Future.successful(()))

    whenReady(OUT.deleteToggle(toggleDetails1.toggleName)) {
      result =>
        result mustBe()
        val captor = ArgumentCaptor.forClass(classOf[String])
        verify(toggleDataRepository, times(1)).deleteFeatureToggle(captor.capture())
        captor.getValue mustBe toggleDetails1.toggleName
    }
  }

  "When getAllFeatureToggles is called returns all of the toggles from the repo" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(seqToggleDetails))
    OUT.getAllFeatureToggles.futureValue mustBe seqToggleDetails
  }

  "When a toggle doesn't exist in the repo, return empty Seq for toggle data repository" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    OUT.getAllFeatureToggles.futureValue mustBe Seq.empty
  }
}
