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

import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "pension-administrator"

ThisBuild / scalaVersion := "3.7.0"
ThisBuild / majorVersion := 0
ThisBuild / scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-feature",
)

val root: Project = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    scalacOptions := scalacOptions.value.distinct,
    CodeCoverageSettings.settings,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies(),
    PlayKeys.devSettings += "play.server.http.port" -> "8205",
    Test / parallelExecution := true,
    RoutesKeys.routesImport ++= Seq(
      "models.SchemeReferenceNumber",
      "models.enumeration.JourneyType",
    ),
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
