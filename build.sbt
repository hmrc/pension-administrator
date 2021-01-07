import AppDependencies.{compile, overrides, test}
import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName: String = ""

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val microservice = Project(AppDependencies.appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(majorVersion := 0)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.11")
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.enumeration.JourneyType",
      "models.FeatureToggleName"
    ),
    libraryDependencies ++= appDependencies,
    dependencyOverrides ++= overrides,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    PlayKeys.devSettings += "play.server.http.port" -> "8205"
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles :=
      "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;.*AuthRetrievals;.*MongoDiagnosticsController;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    fork in Test := true,
    javaOptions in Test += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))