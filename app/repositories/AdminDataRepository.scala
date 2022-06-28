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

package repositories

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import models.FeatureToggle
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.FeatureToggleMongoFormatter.{FeatureToggles, featureToggles, id}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

object FeatureToggleMongoFormatter {
  case class FeatureToggles(_id: String, toggles: Seq[FeatureToggle])

  implicit val featureToggleMongoFormatter: Format[FeatureToggles] = Json.format[FeatureToggles]

  val id = "_id"
  val featureToggles = "toggles"
}

class AdminDataRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     configuration: Configuration
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[FeatureToggles](
    collectionName = configuration.get[String](path = "mongodb.pension-administrator-cache.admin-data.name"),
    mongoComponent = mongoComponent,
    domainFormat = FeatureToggleMongoFormatter.featureToggleMongoFormatter,
    indexes = Seq(
      IndexModel(
        Indexes.ascending(featureToggles),
        IndexOptions().name(featureToggles).unique(true).background(true))
    )
  ) with Logging {


  def getFeatureToggles: Future[Seq[FeatureToggle]] = {
    collection.find[FeatureToggles](
      Filters.eq(id, featureToggles)
    ).headOption().map(_.map(ft =>
      ft.toggles
    ).getOrElse(Seq.empty[FeatureToggle])
    )
  }

  def setFeatureToggles(toggles: Seq[FeatureToggle]): Future[Unit] = {

    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)
    collection.findOneAndUpdate(
      filter = Filters.eq(id, featureToggles),
      update = set(featureToggles, Codecs.toBson(toggles)), upsertOptions)
      .toFuture().map(_ => ())
  }
}