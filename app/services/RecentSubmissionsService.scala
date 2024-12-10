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

package services

import connectors.RecentSubmissionsConnector
import models.recentsubmissions.{ConfirmedDetails, OperatorSubmissionDetails, RecentSubmissionDetails}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RecentSubmissionsService @Inject()(recentSubmissionsConnector: RecentSubmissionsConnector)
                                        (using ExecutionContext) extends Logging {

  def getConfirmedDetailsFor(operatorId: String)
                            (implicit hc: HeaderCarrier): Future[ConfirmedDetails] =
    handleRecentSubmissionWith { submissionDetails =>
      submissionDetails.operatorDetails.get(operatorId).map { operatorSubmissionDetails =>
        ConfirmedDetails(
          businessDetails = operatorSubmissionDetails.businessDetailsCorrect,
          reportingNotifications = operatorSubmissionDetails.reportingNotificationsCorrect.contains(true),
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )
      }.getOrElse(ConfirmedDetails(false, false, submissionDetails.yourContactDetailsCorrect.contains(true)))
    }

  def confirmBusinessDetailsFor(operatorId: String)
                               (implicit hc: HeaderCarrier): Future[ConfirmedDetails] =
    handleRecentSubmissionWith { submissionDetails =>
      val updatedOperatorDetail = submissionDetails.operatorDetails.get(operatorId)
        .map(_.copy(businessDetailsCorrect = true))
        .getOrElse(OperatorSubmissionDetails(operatorId, true, None))

      val updatedDetails = submissionDetails.copy(operatorDetails = submissionDetails.operatorDetails + (operatorId -> updatedOperatorDetail))
      recentSubmissionsConnector.save(updatedDetails)
      ConfirmedDetails(
        businessDetails = updatedOperatorDetail.businessDetailsCorrect,
        reportingNotifications = updatedOperatorDetail.reportingNotificationsCorrect.contains(true),
        yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
      )
    }

  def confirmReportingNotificationsFor(operatorId: String)
                                      (implicit hc: HeaderCarrier): Future[ConfirmedDetails] =
    handleRecentSubmissionWith { submissionDetails =>
      val updatedOperatorDetail = submissionDetails.operatorDetails.get(operatorId)
        .map(_.copy(reportingNotificationsCorrect = Some(true)))
        .getOrElse(OperatorSubmissionDetails(operatorId, false, Some(true)))

      val updatedDetails = submissionDetails.copy(operatorDetails = submissionDetails.operatorDetails + (operatorId -> updatedOperatorDetail))
      recentSubmissionsConnector.save(updatedDetails)
      ConfirmedDetails(
        businessDetails = updatedOperatorDetail.businessDetailsCorrect,
        reportingNotifications = updatedOperatorDetail.reportingNotificationsCorrect.contains(true),
        yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
      )
    }

  def confirmYourContactDetailsFor(operatorId: String)
                                  (implicit hc: HeaderCarrier): Future[ConfirmedDetails] =
    handleRecentSubmissionWith { submissionDetails =>
      val operatorDetail = submissionDetails.operatorDetails
        .getOrElse(operatorId, OperatorSubmissionDetails(operatorId, false, None))

      val updatedSubmissionDetails = submissionDetails.copy(yourContactDetailsCorrect = Some(true))
      recentSubmissionsConnector.save(updatedSubmissionDetails)
      ConfirmedDetails(
        businessDetails = operatorDetail.businessDetailsCorrect,
        reportingNotifications = operatorDetail.reportingNotificationsCorrect.contains(true),
        yourContactDetails = updatedSubmissionDetails.yourContactDetailsCorrect.contains(true)
      )
    }

  private[services] def handleRecentSubmissionWith(block: RecentSubmissionDetails => ConfirmedDetails)
                                                  (implicit hc: HeaderCarrier): Future[ConfirmedDetails] = {
    recentSubmissionsConnector.getRecentUserSubmissions().map {
      case Some(recentSubmissionsResponse) => block(recentSubmissionsResponse)
      case None =>
        recentSubmissionsConnector.save(RecentSubmissionDetails(Map(), None))
        ConfirmedDetails(false, false, false)
    }.recover {
      case NonFatal(e) => logger.warn("Getting recent submission failed", e)
        ConfirmedDetails(false, false, false)
    }
  }
}
