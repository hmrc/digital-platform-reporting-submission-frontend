import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.7.0"
  private val hmrcMongoVersion = "2.4.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"    % "11.11.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"            % hmrcMongoVersion,
    "com.beachape"      %% "enumeratum-play"               % "1.8.0",
    "org.apache.pekko"  %% "pekko-connectors-csv"          % "1.0.2",
    "org.typelevel"     %% "cats-core"                     % "2.12.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"           % "8.1.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
