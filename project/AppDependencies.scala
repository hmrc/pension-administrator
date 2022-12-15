import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.14.1",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % "0.73.0",
    "com.typesafe.play"             %% "play-json"                  % "2.9.3",
    "com.typesafe.play"             %% "play-json-joda"             % "2.9.3",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % "7.11.0",
    "com.networknt"                 % "json-schema-validator"       % "1.0.73",
    "uk.gov.hmrc"                   %% "domain"                     % "8.1.0-play-28",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "7.11.0"    % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.73.0"    % Test,
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"  % "3.5.3"     % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.14"    % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.14.0"  % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.14.0"  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"     % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.62.2"    % scope,
    "org.pegdown"             %  "pegdown"                    % "1.6.0"     % scope,
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.35.0"    % scope,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"      % "1.0.0"     % Test
  )
}
