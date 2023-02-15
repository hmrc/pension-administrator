/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{FeatureToggle, ToggleDetails}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, Updates}
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.TestToggleMongoFormatter.{FeatureToggles, featureToggles, id}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

object TestToggleMongoFormatter {
  case class FeatureToggles(_id: String, toggles: Seq[FeatureToggle])

  implicit val featureToggleMongoFormatter: Format[FeatureToggles] = Json.format[FeatureToggles]

  val id = "_id"
  val featureToggles = "toggles"
}

@Singleton
class ToggleDataRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     configuration: Configuration
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[FeatureToggles](
    collectionName = configuration.get[String](path = "mongodb.pension-administrator-cache.toggle-data.name"),
    mongoComponent = mongoComponent,
    domainFormat = TestToggleMongoFormatter.featureToggleMongoFormatter,
    indexes = Seq(
      IndexModel(
        Indexes.ascending(featureToggles),
        IndexOptions().name(featureToggles).unique(true).background(true))
    )
  ) with Logging {

  def upsertFeatureToggle(toggleDetails: ToggleDetails): Future[Unit] = {
    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)
    collection.findOneAndUpdate(
      filter = Filters.eq("toggleName", toggleDetails.toggleName),
      update = set("toggleProperty", Codecs.toBson(toggleDetails)), upsertOptions)
      .toFuture().map(_ => ())
  }
}
