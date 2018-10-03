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

package controllers

import java.time.Instant
import java.util.Date

import javax.inject.Inject
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{MongoClient, MongoCollection}
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class MongoDiagnosticsController @Inject()(config: Configuration) extends BaseController {

  // scalastyle:off magic.number
  private val banner = Seq.fill(50)("-").mkString
  // scalastyle:on magic.number

  def mongoDiagnostics(): Action[AnyContent] = Action {
    implicit request =>

      Ok(fetchDiagnostics().mkString("\n"))

  }

  def fetchDiagnostics(): Seq[String] = {

    val mongoClient: MongoClient = MongoClient(config.underlying.getString("mongodb.uri"))

    try {
      val db = mongoClient.getDatabase(config.underlying.getString("appName"))

      val result = db.listCollectionNames().toFuture() map {
        names =>
          names.flatMap {
            name =>
              Seq(
                banner,
                name,
                banner
              ) ++
                Await.result(collectionDiagnostics(db.getCollection(name)), 10 seconds)
          }
      }

      Await.result(result, 10 seconds)
    }
    finally {
      mongoClient.close()
    }

  }

  def collectionDiagnostics(collection: MongoCollection[Document]): Future[Seq[String]] = {

    for {
      rows <- rowCount(collection)
      indexes <- indexInfo(collection)
      minLastUpdated <- minLastUpdated(collection)
      externalIds <- ids(collection)
    } yield {
      Seq(
        s"Rows: $rows",
        s"Min Last Updated: $minLastUpdated",
        "",
        "Indexes...",
        indexes,
        "",
        "Ids (External Id, Session Id, etc)...",
        externalIds
      )
    }

  }

  def indexInfo(collection: MongoCollection[Document]): Future[String] = {

    collection.listIndexes().toFuture() map {
      docs =>
        docs.map(_.toJson()).mkString("\n")
    }

  }

  def minLastUpdated(collection: MongoCollection[Document]): Future[String] = {

    collection.aggregate(Seq(group("", min("minLastUpdated", "$lastUpdated")))).head() map {
      doc =>
        if (doc != null) {
          (Json.parse(doc.toJson()) \ "minLastUpdated" \ "$date").validateOpt[Long] match {
            case JsSuccess(Some(minLastUpdated), _) => Date.from(Instant.ofEpochMilli(minLastUpdated)).toString
            case JsSuccess(None, _) => "<none>"
            case JsError(errors) => throw JsResultException(errors)
          }
        } else {
          "<none>"
        }
    }

  }

  def ids(collection: MongoCollection[Document]): Future[String] = {

    collection.find().projection(include("id")).toFuture() map {
      docs =>
        docs map {
          doc =>
            (Json.parse(doc.toJson()) \ "id").validate[String] match {
              case JsSuccess(externalId, _) => externalId
              case JsError(errors) => throw JsResultException(errors)
            }
        } mkString "\n"
    }

  }

  def rowCount(collection: MongoCollection[Document]): Future[Long] = {

    collection.countDocuments().head()

  }

}
