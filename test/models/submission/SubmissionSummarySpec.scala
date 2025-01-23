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

package models.submission

import models.submission.SubmissionStatus._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.{Instant, Year}

class SubmissionSummarySpec extends AnyFreeSpec with Matchers {

  ".link" - {
    
    implicit val messages: Messages = stubMessages()
    
    val instant = Instant.now()
    val baseSubmission = SubmissionSummary(
      submissionId = "id",
      fileName = "filename",
      operatorId = Some("operatorId"),
      operatorName = Some("operatorName"),
      reportingPeriod = Some(Year.of(2024)),
      submissionDateTime = instant,
      submissionStatus = Rejected,
      assumingReporterName = None,
      submissionCaseId = Some("caseId"),
      localDataExists = true
    )

    "for rejected submissions" - {

      "must be present when local data exists and operatorId is populated" in {
        baseSubmission.link mustBe defined
      }

      "must not be present when local data exists and operatorId is None" in {
        val submission = baseSubmission.copy(operatorId = None)
        submission.link must not be defined
      }

      "must not be present when local data does not exist" in {
        val submission = baseSubmission.copy(localDataExists = false)
        submission.link must not be defined
      }
    }

    "for successful submissions" - {

      "must be present when local data exists and operatorId is populated" in {

        val submission = baseSubmission.copy(submissionStatus = Success)
        submission.link mustBe defined
      }

      "must not be present when local data exists and operatorId is None" in {

        val submission = baseSubmission.copy(submissionStatus = Success, operatorId = None)
        submission.link must not be defined
      }

      "must not be present when local data does not exist" in {

        val submission = baseSubmission.copy(submissionStatus = Success, localDataExists = false)
        submission.link must not be defined
      }
    }

    "for pending submissions" - {

      "must be present when local data exists and operatorId is populated" in {

        val submission = baseSubmission.copy(submissionStatus = Pending)
        submission.link mustBe defined
      }

      "must not be present when local data exists and operatorId is None" in {

        val submission = baseSubmission.copy(submissionStatus = Pending, operatorId = None)
        submission.link must not be defined
      }

      "must not be present when local data does not exist" in {

        val submission = baseSubmission.copy(submissionStatus = Pending, localDataExists = false)
        submission.link must not be defined
      }
    }
  }
}
