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

package controllers.submission

import base.SpecBase
import connectors.SubmissionConnector
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.http.StringContextOps
import viewmodels.PlatformOperatorSummary
import views.html.submission.CheckFileView

import java.time.{Instant, Year}
import scala.concurrent.Future

class CheckFileControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "CheckFile Controller" - {

    "onPageLoad" - {

      "when there is a submission in a submitted state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val operatorName = "operator"

          val platformOperatorSummary = PlatformOperatorSummary(operatorId, operatorName, true)
          val answers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            state = Submitted("test.xml"),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {

            given Messages = messages(application)

            val expectedSummaryList = SummaryList(
              rows = Seq(
                SummaryListRow(
                  key = Key(content = Text(Messages("checkFile.fileName"))),
                  value = Value(content = Text("test.xml")),
                )
              )
            )

            val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value
            val view = application.injector.instanceOf[CheckFileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(operatorId, "id", expectedSummaryList, operatorName)(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when there is no submission for the given id" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(None))

          running(application) {
            val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when the submission is in a ready state" - {

          "must redirect to the upload page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = Ready,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an uploading state" - {

          "must redirect to the uploading page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = Uploading,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadingController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an upload failed state" - {

          "must redirect to the upload failed page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = UploadFailed("reason"),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a validated state" - {

          "must redirect to the send file page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = Validated(
                downloadUrl = url"http://example.com/test.xml",
                platformOperatorId = "poid",
                reportingPeriod = Year.of(2024),
                fileName = "test.xml",
                checksum = "checksum",
                size = 1337L
              ),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an approved state" - {

          "must redirect to the file passed page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = Approved,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a rejected state" - {

          "must redirect to the file failed page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              state = Rejected,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.FileErrorsController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }
      }

      "when there are no user answers" - {

        "must redirect to Journey Recovery" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.CheckFileController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).get(any())(using any())
        }
      }
    }
  }
}
