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

object MicroServiceBuild extends Build with MicroService {

  val appName = "pension-administrator"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  private val reactiveMongoVersion = "6.2.0"
  private val bootstrapVersion = "4.4.0"
  private val schemaValidatorVersion = "0.1.19"
  private val jsonPathVersion = "2.5.0"
  private val domainVersion = "5.2.0"

  private val hmrcTestVersion = "3.3.0"
  private val scalaTestVersion = "3.0.4"
  private val pegDownVersion = "1.6.0"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val mockitoAllVersion = "1.10.19"
  private val wireMockVersion = "2.18.0"
  private val scalacheckVersion = "1.14.0"
  private val scalacheckGenRegexp = "0.1.1"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % reactiveMongoVersion,
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapVersion,
    "com.networknt" % "json-schema-validator" % schemaValidatorVersion,
    "com.josephpconley" %% "play-jsonpath" % jsonPathVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion
  )

  def test(scope: String = "test,it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
    "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
    "org.pegdown" % "pegdown" % pegDownVersion % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope,
    "wolfendale" %% "scalacheck-gen-regexp" % scalacheckGenRegexp % scope
  )

}
