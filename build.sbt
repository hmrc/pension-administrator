import AppDependencies.{compile, test}
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings


val appName: String = ""

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

lazy val microservice = Project(AppDependencies.appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(majorVersion := 0)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.13.8")
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(libraryDependencies ++= appDependencies)
  .settings(retrieveManaged := true)
  .settings(PlayKeys.devSettings += "play.server.http.port" -> "8205")
  .settings(update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false))
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "models.enumeration.JourneyType",
      "models.FeatureToggleName"
    ),
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    Test / parallelExecution := true
  )
  .settings(CodeCoverageSettings.settings: _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
