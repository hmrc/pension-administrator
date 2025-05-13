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

import AppDependencies.{compile, test}
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

lazy val microservice = Project(AppDependencies.appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(majorVersion := 0)
  .settings(scalaSettings *)
  .settings(scalaVersion := "2.13.16")
  .settings(defaultSettings() *)
  .settings(libraryDependencies ++= appDependencies)
  .settings(retrieveManaged := true)
  .settings(PlayKeys.devSettings += "play.server.http.port" -> "8205")
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.SchemeReferenceNumber",
      "models.enumeration.JourneyType"
    ),
    scalacOptions ++= Seq(
    "-Wconf:src=routes/.*:s",
    //"-Xfatal-warnings", //added for future-proofing; disabled temporarily until scala3 aspect of ticket
    "-Wunused:params",
    "-Wunused:implicits",
    "-Wunused:imports",
    "-feature"
    )
  )
  .settings(
    Test / parallelExecution := true
  )
  .settings(CodeCoverageSettings.settings *)
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
