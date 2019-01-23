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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  private val scalaTestPlusPlayVersion = "3.1.2"
  private val mockitoAllVersion = "1.10.19"
  private val wireMockVersion = "2.19.0"
  private val scalacheckVersion = "1.14.0"
  private val domainVersion = "5.2.0"
  private val scalacheckGenRegexp = "0.1.1"
  private val bootstrapVersion = "0.32.0"
  private val playJsonVersion = "2.6.10"
  private val scalaTestVersion = "3.0.5"
  private val reactiveMongoVersion = "7.9.0-play-26"
  private val jsonSchemeValidatorVersion = "0.1.19"
  private val jsonPathVersion = "2.5.0"
  private val reactiveMongoTestVersion = "4.2.0-play-26"
  private val hmrcTestVersion = "3.2.0"
  private val pegDownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "simple-reactivemongo"   % reactiveMongoVersion,
    "com.typesafe.play"       %% "play-json"              % playJsonVersion,
    "com.typesafe.play"       %% "play-json-joda"         % playJsonVersion,
    "uk.gov.hmrc"             %% "bootstrap-play-26"      % bootstrapVersion,
    "com.networknt"           %  "json-schema-validator"  % jsonSchemeValidatorVersion,
    "com.josephpconley"       %% "play-jsonpath"          % jsonPathVersion,
    "uk.gov.hmrc"             %% "domain"                 % domainVersion
  )

  def test(scope: String = "test,it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-play-26"            % bootstrapVersion          % Test classifier "tests",
    "uk.gov.hmrc"                 %% "reactivemongo-test"           % reactiveMongoTestVersion  % Test,
    "uk.gov.hmrc"                 %% "hmrctest"                     % hmrcTestVersion           % scope,
    "org.scalatest"               %% "scalatest"                    % scalaTestVersion          % scope,
    "org.pegdown"                  % "pegdown"                      % pegDownVersion            % scope,
    "org.scalacheck"              %% "scalacheck"                   % scalacheckVersion         % scope,
    "com.typesafe.play"           %% "play-test"                    % PlayVersion.current       % scope,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % scalaTestPlusPlayVersion  % scope,
    "org.mockito"                  % "mockito-all"                  % mockitoAllVersion         % scope,
    "com.github.tomakehurst"       % "wiremock"                     % wireMockVersion           % scope,
    "wolfendale"                  %% "scalacheck-gen-regexp"        % scalacheckGenRegexp       % scope
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Set[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Set(
      "reactivemongo"              %% "reactivemongo"      % "0.16.1", //TODO REMOVE OVERRIDE AFTER FIX IS IMPLEMENTED ON REACTIVE MONGO
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }

}
