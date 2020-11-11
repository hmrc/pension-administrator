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

package service

import akka.Done
import base.SpecBase
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.{IntegrationFramework, SomeOtherToggle}
import models.{FeatureToggle, FeatureToggleName, OperationFailed, OperationSucceeded}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.cache.AsyncCacheApi
import repositories.AdminDataRepository

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.concurrent.ExecutionContext.Implicits.global

class FeatureToggleServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with MustMatchers {
  implicit private val arbitraryFeatureToggleName: Arbitrary[FeatureToggleName] =
    Arbitrary {
      Gen.oneOf(FeatureToggleName.toggles)
    }

  implicit private val arbitraryFeatureToggle: Arbitrary[FeatureToggle] =
    Arbitrary {
      for {
        name <- arbitrary[FeatureToggleName]
        enabled <- arbitrary[Boolean]
      } yield FeatureToggle(name, enabled)
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
    val adminDataRepository = mock[AdminDataRepository]
    when(adminDataRepository.getFeatureToggles()).thenReturn(Future.successful(Seq.empty))
    when(adminDataRepository.setFeatureToggles(any())).thenReturn(Future.successful(true))

    val OUT = new FeatureToggleService(adminDataRepository, new FakeCache())
    val toggleName = arbitrary[FeatureToggleName].sample.value

    whenReady(OUT.set(toggleName = toggleName, enabled = true)) {
      result =>
        result mustBe OperationSucceeded
        val captor = ArgumentCaptor.forClass(classOf[Seq[FeatureToggle]])
        verify(adminDataRepository, times(1)).setFeatureToggles(captor.capture())
        captor.getValue must contain(Enabled(toggleName))
    }
  }

  "When set fails in the repo returns a success result" in {
    val adminDataRepository = mock[AdminDataRepository]
    val toggleName = arbitrary[FeatureToggleName].sample.value

    val OUT = new FeatureToggleService(adminDataRepository, new FakeCache())

    when(adminDataRepository.getFeatureToggles()).thenReturn(Future.successful(Seq.empty))
    when(adminDataRepository.setFeatureToggles(any())).thenReturn(Future.successful(false))

    whenReady(OUT.set(toggleName = toggleName, enabled = true))(_ mustBe OperationFailed)
  }

  "When getAll is called returns all of the toggles from the repo" in {
    val adminDataRepository = mock[AdminDataRepository]
    val OUT = new FeatureToggleService(adminDataRepository, new FakeCache())

    when(adminDataRepository.getFeatureToggles()).thenReturn(Future.successful(Seq.empty))

    OUT.getAll.futureValue mustBe Seq(
      Disabled(IntegrationFramework),
      Disabled(SomeOtherToggle)
    )
  }

  "When a toggle doesn't exist in the repo, return default" in {
    val adminDataRepository = mock[AdminDataRepository]
    when(adminDataRepository.getFeatureToggles()).thenReturn(Future.successful(Seq.empty))
    val OUT = new FeatureToggleService(adminDataRepository, new FakeCache())
    OUT.get(IntegrationFramework).futureValue mustBe Disabled(IntegrationFramework)
  }

  "When a toggle exists in the repo, override default" in {
    val adminDataRepository = mock[AdminDataRepository]
    when(adminDataRepository.getFeatureToggles()).thenReturn(Future.successful(Seq(Enabled(IntegrationFramework))))
    val OUT = new FeatureToggleService(adminDataRepository, new FakeCache())
    OUT.get(IntegrationFramework).futureValue mustBe Enabled(IntegrationFramework)
  }
}
