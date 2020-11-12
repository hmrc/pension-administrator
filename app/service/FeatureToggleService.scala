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

import javax.inject.{Inject, Singleton}
import models.FeatureToggle._
import models.FeatureToggleName.{IntegrationFramework, SomeOtherToggle}
import models._
import play.api.cache.AsyncCacheApi
import repositories.AdminDataRepository

import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS => Seconds}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureToggleService @Inject()(
                                      adminDataRepository: AdminDataRepository,
                                      cacheApi: AsyncCacheApi
                                    )(implicit ec: ExecutionContext) {
  private val cacheValidFor: FiniteDuration = Duration(2, Seconds)

  private val defaults: Seq[FeatureToggle] = Seq(
    Disabled(IntegrationFramework),
    Disabled(SomeOtherToggle)
  )

  private def addDefaults(fromDb: Seq[FeatureToggle]): Seq[FeatureToggle] = {
    val toAdd = defaults.filterNot(d => fromDb.exists(fdb => fdb.name == d.name))
    fromDb ++ toAdd
  }

  def getAll: Future[Seq[FeatureToggle]] =
    cacheApi.getOrElseUpdate[Seq[FeatureToggle]]("toggles", cacheValidFor) {
      adminDataRepository
        .getFeatureToggles
        .map(addDefaults)
    }

  def set(toggleName: FeatureToggleName, enabled: Boolean): Future[BinaryResult] =
    getAll.flatMap {
      currentToggles =>
        val newToggles = currentToggles
          .filterNot(toggle => toggle.name == toggleName) :+ FeatureToggle(toggleName, enabled)

        adminDataRepository.setFeatureToggles(newToggles).map {
          case true => OperationSucceeded
          case false => OperationFailed
        }
    }

  def get(name: FeatureToggleName): Future[FeatureToggle] =
    getAll.map {
      toggles =>
        toggles.find(_.name == name).getOrElse(Disabled(name))
    }
}
