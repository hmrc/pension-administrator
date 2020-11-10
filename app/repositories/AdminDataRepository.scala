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

package repositories

import com.google.inject.Inject
import models.FeatureToggle
import play.api.libs.json._
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

class AdminDataRepository @Inject()(
                                     mongoComponent: ReactiveMongoComponent,
                                     configuration: Configuration
                                   )(implicit val ec: ExecutionContext) extends ReactiveRepository[JsValue, BSONObjectID](
  configuration.get[String](path = "mongodb.pension-administrator-cache.admin-data.name"),
  mongoComponent.mongoConnector.db,
  implicitly
) {

  val featureToggleDocumentId = "toggles"

  val collectionIndexes = Seq(
//    Index(
//      key = Seq(("id", IndexType.Ascending)),
//      name = Some("id"),
//      background = true,
//      unique = true
//    ),
    Index(
      key = Seq((featureToggleDocumentId, IndexType.Ascending)),
      name = Some(featureToggleDocumentId),
      background = true,
      unique = true
    )
  )

  (for {
    _ <- createIndex(collectionIndexes)
  } yield {
    ()
  }) recoverWith {
    case t: Throwable =>
      Future.successful(Logger.error(s"Error creating indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }


  private def createIndex(indexes: Seq[Index]): Future[Seq[Boolean]] = {
    Future.sequence(
      indexes.map { index =>
        collection.indexesManager.ensure(index) map { result =>
          Logger.debug(message = s"Index $index was created successfully and result is: $result")
          result
        } recover {
          case e: Exception => Logger.error(message = s"Failed to create index $index", e)
            false
        }
      }
    )
  }

  def getFeatureToggles()
                       (implicit ec: ExecutionContext): Future[Seq[FeatureToggle]] = {

    implicit val r: Reads[Option[FeatureToggle]] = Reads.optionNoError(FeatureToggle.reads)

    collection
      .find(
        selector = BSONDocument("id" -> featureToggleDocumentId),
        projection = Option.empty[JsObject]
      ).one[JsObject].map(_.map(
      js =>
        (js \ "toggles").as[Seq[Option[FeatureToggle]]].collect {
          case Some(toggle) => toggle
        }
    )).map(_.getOrElse(Seq.empty[FeatureToggle]))
  }

  def setFeatureToggles(toggles: Seq[FeatureToggle])
                       (implicit ec: ExecutionContext): Future[Boolean] = {

    val selector = Json.obj(
      "_id" -> featureToggleDocumentId
    )

    val modifier = Json.obj(
      "_id" -> featureToggleDocumentId,
      "toggles" -> Json.toJson(toggles)
    )

    collection.update(ordered = false)
      .one(selector, modifier, upsert = true).map {
      lastError: WriteResult =>
        lastError.ok
    }
  }
}