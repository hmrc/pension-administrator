import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val appName = "pension-administrator"
  private val bootstrapVersion = "9.5.0"
  private val mongoVersion = "2.2.0"
  val compile: Seq[ModuleID] = Seq(
    ws,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.17.2",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "com.typesafe.play"             %% "play-json"                  % "2.10.5",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"                 % "json-schema-validator"       % "1.5.1",
    "uk.gov.hmrc"                   %% "domain-play-30"             % "10.0.0",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion        % Test,
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"  % "4.16.2"     % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.19"    % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.18.0"  % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0"  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.1"     % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"    % scope,
    "org.pegdown"             %  "pegdown"                    % "1.6.0"     % scope,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"      % "1.1.0"     % Test
  )
}
