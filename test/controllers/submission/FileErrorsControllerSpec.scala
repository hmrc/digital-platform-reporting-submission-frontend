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
import models.submission.*
import models.submission.Submission.State.*
import models.submission.Submission.SubmissionType
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.StringContextOps
import views.html.submission.{FileErrorsNoDownloadView, FileErrorsView}

import java.time.{Instant, Year}
import scala.concurrent.Future

class FileErrorsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "FileErrors Controller" - {

    "onPageLoad" - {

      "when there is a submission in a rejected state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value
            val view = application.injector.instanceOf[FileErrorsView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(submission.operatorId, submission._id, "test.xml")(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when there is no submission for the given id" - {

        "must return NotFound" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(None))

          running(application) {
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual NOT_FOUND
          }
        }
      }

      "when the submission is in a ready state" - {

        "must redirect to the upload page" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when the submission is in an uploading state" - {

        "must redirect to the uploading page" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadingController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when the submission is in an upload failed state" - {

        "must redirect to the upload failed page" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when the submission is in a validated state" - {

        "must redirect to the send file page" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when the submission is in a submitted state" - {

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

        val expectedViewRequest = ViewSubmissionsRequest(
          assumedReporting = false,
          pageNumber = 1,
          sortBy = SortBy.SubmissionDate,
          sortOrder = SortOrder.Descending,
          reportingPeriod = None,
          operatorId = None,
          statuses = Nil,
          fileName = Some("test")
        )

        "and no submissions are found at CADX" - {

          "must redirect to Check File" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(None))

            running(application) {
              val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).listDeliveredSubmissions(eqTo(expectedViewRequest))(using any)
          }
        }

        "and this submission is not found at CADX" - {

          "must redirect to Check File" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(SubmissionsSummary(Nil, 0, false, 0))))

            running(application) {
              val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).listDeliveredSubmissions(eqTo(expectedViewRequest))(using any)
          }
        }

        "and the submission from CADX is in an approved state" - {

          "must redirect to Submission Confirmation" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submissionSummary = SubmissionSummary(
              submissionId = "id",
              fileName = "test.xml",
              operatorId = "operatorId",
              operatorName = "operatorName",
              reportingPeriod = Year.of(2024),
              submissionDateTime = now,
              submissionStatus = SubmissionStatus.Success,
              assumingReporterName = None,
              submissionCaseId = Some("caseId"),
              localDataExists = true
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(SubmissionsSummary(Seq(submissionSummary), 1, false, 0))))

            running(application) {
              val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).listDeliveredSubmissions(eqTo(expectedViewRequest))(using any)
          }
        }

        "and the submission from CADX is in a pending state" - {

          "must redirect to Check File" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submissionSummary = SubmissionSummary(
              submissionId = "id",
              fileName = "test.xml",
              operatorId = "operatorId",
              operatorName = "operatorName",
              reportingPeriod = Year.of(2024),
              submissionDateTime = now,
              submissionStatus = SubmissionStatus.Pending,
              assumingReporterName = None,
              submissionCaseId = Some("caseId"),
              localDataExists = true
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(SubmissionsSummary(Seq(submissionSummary), 1, false, 0))))

            running(application) {
              val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).listDeliveredSubmissions(eqTo(expectedViewRequest))(using any)
          }
        }

        "and the submission from CADX is in a rejected state" - {

          "must render the correct view" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val submissionSummary = SubmissionSummary(
              submissionId = "id",
              fileName = "test.xml",
              operatorId = "operatorId",
              operatorName = "operatorName",
              reportingPeriod = Year.of(2024),
              submissionDateTime = now,
              submissionStatus = SubmissionStatus.Rejected,
              assumingReporterName = None,
              submissionCaseId = Some("caseId"),
              localDataExists = true
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(SubmissionsSummary(Seq(submissionSummary), 1, false, 0))))

            running(application) {
              val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value
              val view = application.injector.instanceOf[FileErrorsNoDownloadView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(submission.operatorId, submission._id, "test.xml")(request, messages(application)).toString
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).listDeliveredSubmissions(eqTo(expectedViewRequest))(using any)
          }
        }
      }

      "when the submission is in a approved state" - {

        "must redirect to the file success page" in {

          val application = applicationBuilder(userAnswers = None)
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
            val request = FakeRequest(routes.FileErrorsController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }
    }

    "listErrors" - {

      "when there is a submission in a rejected state for the given id" - {

        "must return OK and the errors as a TSV download" in {

          val application = applicationBuilder(userAnswers = None)
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

          val error1 = CadxValidationError.FileError(submissionId = "id", dprsId = "dprsId", code = "001", detail = Some("some detail"), created = now)
          val error2 = CadxValidationError.RowError(submissionId = "id", dprsId = "dprsId", code = "002", docRef = "docRef", detail = Some("some detail"), created = now)

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockSubmissionConnector.getErrors(any())(using any())).thenReturn(Future.successful(Source(Seq(error1, error2))))

          running(application) {
            given Materializer = application.materializer

            val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
            val result = route(application, request).value
            val expectedBodySource = scala.io.Source.fromFile(getClass.getResource("/example-errors.tsv").toURI)
            val expectedBody = expectedBodySource.mkString
            expectedBodySource.close()

            status(result) mustEqual OK
            contentType(result).value mustEqual "text/tsv"
            header("Content-Disposition", result).value mustEqual """attachment; filename="test_xml-errors.tsv""""
            contentAsString(result) mustEqual expectedBody
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when there is no submission for the given id" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(None))

          running(application) {
            val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when the submission is in a ready state" - {

          "must redirect to the upload page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an uploading state" - {

          "must redirect to the uploading page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadingController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an upload failed state" - {

          "must redirect to the upload failed page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a validated state" - {

          "must redirect to the send file page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in an submitted state" - {

          "must redirect to the check file page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a approved state" - {

          "must redirect to the file success page" in {

            val application = applicationBuilder(userAnswers = None)
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
              val request = FakeRequest(routes.FileErrorsController.listErrors(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }
      }
    }
  }
}
