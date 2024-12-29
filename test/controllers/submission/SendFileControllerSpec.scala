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
import controllers.routes as baseRoutes
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.submission.Submission.SubmissionType
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryListRow}
import uk.gov.hmrc.http.StringContextOps
import views.html.submission.SendFileView

import java.time.{Instant, Year}
import scala.concurrent.Future

class SendFileControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "SendFile Controller" - {

    "onPageLoad" - {

      "when there is a submission in a validated state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            submissionType = SubmissionType.Xml,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            assumingOperatorName = None,
            state = Validated(
              downloadUrl = url"http://example.com/test.xml",
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

            given Messages = messages(application)

            val expectedSummaryList = SummaryList(
              rows = Seq(
                SummaryListRow(
                  key = Key(content = Text(Messages("sendFile.uploadedFile"))),
                  value = Value(content = Text("test.xml")),
                  actions = Some(Actions(items = Seq(
                    ActionItem(href = routes.UploadController.onRedirect(operatorId, "id").url, content = Text(Messages("site.change")), visuallyHiddenText = Some(Messages("sendFile.uploadedFile.change")))
                  )))
                ),
                SummaryListRow(
                  key = Key(content = Text(Messages("sendFile.operatorName"))),
                  value = Value(content = Text("operatorName"))
                ),
                SummaryListRow(
                  key = Key(content = Text(Messages("sendFile.operatorId"))),
                  value = Value(content = Text(operatorId))
                ),
                SummaryListRow(
                  key = Key(content = Text(Messages("sendFile.reportingPeriod"))),
                  value = Value(content = Text("2024"))
                )
              ),
              classes = "govuk-!-margin-bottom-8"
            )

            val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[SendFileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(operatorId, "id", expectedSummaryList)(request, messages(application)).toString
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
            val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Ready,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Uploading,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = UploadFailed(SchemaValidationError(Seq.empty, false), None),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a submitted state" - {

          "must redirect to the checking file page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Submitted("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Approved("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Rejected("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
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
            val request = FakeRequest(routes.SendFileController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).get(any())(using any())
        }
      }

      "when submissions are disabled" - {

        "must redirect to SubmissionsDisabled" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

          running(application) {

            val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
          }
        }
      }
    }

    "onSubmit" - {

      "when there is a submission in a validated state for the given id" - {

        "must initiate the submission and return SEE_OTHER and redirect to the check file page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            submissionType = SubmissionType.Xml,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            assumingOperatorName = None,
            state = Validated(
              downloadUrl = url"http://example.com/test.xml",
              reportingPeriod = Year.of(2024),
              fileName = "test.xml",
              checksum = "checksum",
              size = 1337L
            ),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, submission._id).url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockSubmissionConnector).submit(eqTo("id"))(using any())
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
            val request = FakeRequest(GET, routes.SendFileController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).submit(eqTo("id"))(using any())
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Ready,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Uploading,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadingController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = UploadFailed(SchemaValidationError(Seq.empty, false), None),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
          }
        }

        "when the submission is in a submitted state" - {

          "must redirect to the checking file page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Submitted("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Approved("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
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
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              assumingOperatorName = None,
              state = Rejected("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, submission._id))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.FileErrorsController.onPageLoad(operatorId, submission._id).url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector, never()).submit(any())(using any())
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
            val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).get(any())(using any())
        }
      }

      "when submissions are disabled" - {

        "must redirect to SubmissionsDisabled" in {

          val application = applicationBuilder(userAnswers = None)
            .configure("features.submissions-enabled" -> false)
            .build()

          running(application) {
            val request = FakeRequest(routes.SendFileController.onSubmit(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
          }
        }
      }
    }
  }
}
