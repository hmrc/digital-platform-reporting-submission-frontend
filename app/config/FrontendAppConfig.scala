/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) {

  val host: String = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")
  val baseUrl: String = configuration.get[String]("platform.frontend.host")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "digital-platform-reporting-submission-frontend"

  val digitalPlatformReportingUrl: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String = configuration.get[String]("urls.signOut")

  private val exitSurveyBaseUrl: String = configuration.get[String]("feedback-frontend.host")
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/digital-platform-reporting-submission-frontend"

  val languageTranslationEnabled: Boolean = configuration.get[Boolean]("features.welsh-translation")
  val dataEncryptionEnabled: Boolean = configuration.get[Boolean]("features.use-encryption")
  val extendedCountriesListEnabled: Boolean = configuration.get[Boolean]("features.extended-countries-list")
  val submissionsEnabled: Boolean = configuration.get[Boolean]("features.submissions-enabled")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val upscanCallbackDelayInSeconds: Int = configuration.get[Int]("microservice.services.upscan-initiate.callbackDelayInSeconds")
  
  private val operatorFrontendUrl: String = configuration.get[String]("microservice.services.digital-platform-reporting-operator-frontend.baseUrl")
  val addOperatorUrl: String = s"$operatorFrontendUrl/platform-operator/add-platform-operator/start"

  def updateOperatorUrl(operatorId: String) = s"$operatorFrontendUrl/platform-operator/$operatorId/initialise-check-your-answers"

  def viewNotificationsUrl(operatorId: String) = s"$operatorFrontendUrl/reporting-notification/$operatorId/initialise-view"

  def addReportingNotificationUrl(operatorId: String) = s"$operatorFrontendUrl/reporting-notification/$operatorId/start"

  private val manageFrontendUrl: String = configuration.get[String]("microservice.services.digital-platform-reporting-manage-frontend.baseUrl")
  val updateContactDetailsUrl: String = s"$manageFrontendUrl/contact-details/view-contact-details"
  val manageHomepageUrl: String = s"$manageFrontendUrl/manage-reporting"

  val emailServiceUrl: String = configuration.get[Service]("microservice.services.email").baseUrl
}
