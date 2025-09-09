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

package repositories

import com.google.inject.Inject
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.{ExecutionContext, Future}

class DropMongoCollections @Inject()(
  mongoComponent: MongoComponent,
  configuration: Configuration
)(implicit ec: ExecutionContext)
  extends Logging {

  private def collectionNamesToDrop: Seq[String] =
    configuration.getOptional[Seq[String]]("mongodb.collections-to-drop").getOrElse(Seq.empty[String])

  private def filteredCollectionNames: Future[Seq[Option[String]]] =
    if (collectionNamesToDrop.isEmpty) {
      logger.info("collectionNamesToDrop empty")

      Future.successful(Seq(None))
    } else {
      mongoComponent.database.listCollectionNames().collect().head().map {
        existingCollectionNames =>
          collectionNamesToDrop.map {
            collectionNameToDrop =>
              existingCollectionNames.find(p => p.equals(collectionNameToDrop))
          }
      }
    }

  private def dropCollections: Future[Unit] =
    filteredCollectionNames.map {
      collectionNames =>
        if (collectionNames.flatten.nonEmpty) {
          logger.info(s"collections to drop: ${collectionNames.flatten.mkString(", ")}")

          collectionNames.flatten.foreach {
            collectionName =>
              logger.info(s"dropping $collectionName...")

              mongoComponent.database.getCollection(collectionName).drop().headOption().map {
                case Some(_) =>
                  logger.info(s"$collectionName dropped")
                case _ =>
                  logger.info(s"$collectionName not dropped")
              }
          }
        } else {
          Future.unit
        }
    }

  dropCollections
}
