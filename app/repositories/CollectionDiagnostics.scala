/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logger
import reactivemongo.api.ReadConcern
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class IndexDef(name: String, fields: Seq[String], unique: Boolean, ttl: Option[Int])

object CollectionDiagnostics {

  def logCollectionInfo(collection: JSONCollection): Unit = {

    indexInfo(collection) map {
      indexes =>
        Logger.warn(
          s"Diagnostic information for collection ${collection.name}\n\n" +
          s"Index definitions\n\n" +
            (indexes.map {
              index =>
                s"Name:   ${index.name}\n" +
                s"Fields: ${index.fields.mkString(", ")}\n" +
                s"Unique: ${index.unique}\n" +
                s"TTL:    ${index.ttl.getOrElse("<none>")}\n"
            } mkString "\n")
        )
    }

    collection.count(
      selector = None,
      limit = None,
      skip = 0,
      hint = None,
      readConcern = ReadConcern.Available
    ).foreach { count =>
      Logger.warn(
        s"\nRow count for collection ${collection.name} : $count\n\n"
      )
    }
  }

  def indexInfo(collection: JSONCollection): Future[Seq[IndexDef]] = {

    collection.indexesManager.list().map {
      indexes =>
        indexes.map {
          index =>
            val ttl = index.options.getAs[Int]("expireAfterSeconds")
            IndexDef(
              index.eventualName,
              index.key.map(_._1),
              index.unique,
              ttl
            )
        }
    }

  }

  def checkIndexTtl(collection: JSONCollection, indexName: String, ttl: Option[Int]): Future[Unit] = {

    indexInfo(collection)
      .flatMap {seqIndexes =>
        seqIndexes
          .find(index => index.name == indexName && index.ttl != ttl)
          .map {
            index =>
              Logger.warn(s"Index $indexName on collection ${collection.name} with TTL ${index.ttl} does not match configuration value $ttl")
              collection.indexesManager.drop(index.name) map {
                case n if n > 0 => Logger.warn(s"Dropped index $indexName on collection ${collection.name} as TTL value incorrect")
                case _ => Logger.warn(s"Index index $indexName on collection ${collection.name} had already been dropped (possible race condition)")
              }
          } getOrElse Future.successful(Logger.info(s"Index $indexName on collection ${collection.name} has correct TTL $ttl"))
      }

  }

}
