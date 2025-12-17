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

import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  private val mongoVersion = "2.11.0"

  private val compile: Seq[ModuleID] = Seq(
    ws,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.20.0",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"                 %  "json-schema-validator"      % "1.5.8",
    "uk.gov.hmrc"                   %% "domain-play-30"             % "13.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion,
    "org.scalatest"           %% "scalatest"                  % "3.2.19",
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.18.0",
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.2",
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"      % "1.1.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
