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
import connectors.{PlatformOperatorConnector, SubmissionConnector, SubscriptionConnector}
import forms.SubmissionConfirmationFormProvider
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.submission.Submission.SubmissionType
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import models.subscription.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.StringContextOps
import views.html.submission.SubmissionConfirmationView

import java.time.{Instant, LocalDateTime, Year, ZoneOffset}
import scala.concurrent.Future

class SubmissionConfirmationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector, mockSubscriptionConnector, mockConnector)
  }

  "SubmissionConfirmation Controller" - {

    "onPageLoad" - {
      val contact = IndividualContact(Individual("first", "last"), "tax1@team.com", None)
      val subscription = SubscriptionInfo(
        id = "dprsId",
        gbUser = true,
        tradingName = None,
        primaryContact = contact,
        secondaryContact = None
      )

      val operator = PlatformOperator(
        operatorId = "operatorId",
        operatorName = "operatorName",
        tinDetails = Nil,
        businessName = None,
        tradingName = None,
        primaryContactDetails = ContactDetails(None, "name", "tax2@team.com"),
        secondaryContactDetails = None,
        addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
        notifications = Nil
      )
      "when there is a submission in an approved state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
            )
            .build()

          val updatedInstant = LocalDateTime.of(2024, 2, 1, 12, 30, 0, 0).toInstant(ZoneOffset.UTC)

          val submission = Submission(
            _id = "id",
            submissionType = SubmissionType.Xml,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = Approved("test.xml", Year.of(2024)),
            created = now,
            updated = updatedInstant
          )
          when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
          when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value
            val view = application.injector.instanceOf[SubmissionConfirmationView]
            val form = application.injector.instanceOf[SubmissionConfirmationFormProvider].apply(operatorName)

            given Messages = messages(application)
            val expectedSummaryList = {
              SummaryList(
                rows = Seq(
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.fileName"))),
                    value = Value(content = Text("test.xml")),
                  ),
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.operatorName"))),
                    value = Value(content = Text(operatorName)),
                  ),
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.operatorId"))),
                    value = Value(content = Text(operatorId)),
                  ),
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.reportingPeriod"))),
                    value = Value(content = Text("2024")),
                  ),
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.checksCompleted"))),
                    value = Value(content = Text("12:30pm GMT on 1 February 2024")),
                  ),
                  SummaryListRow(
                    key = Key(content = Text(Messages("submissionConfirmation.dprsId"))),
                    value = Value(content = Text("dprsId"))
                  ),
                )
              )
            }
            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, operatorId, operatorName, "id", expectedSummaryList, subscription.primaryContact.email, operator.primaryContactDetails.emailAddress)(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
        }
      }

      "when there is no submission for the given id" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
            )
            .build()

          when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
          when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(None))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when the submission is in a ready state" - {

          "must redirect to the upload page" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Ready,
              created = now,
              updated = now
            )
            
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
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
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Uploading,
              created = now,
              updated = now
            )
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
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
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = UploadFailed(SchemaValidationError(Seq.empty, false), None),
              created = now,
              updated = now
            )
            
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
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
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Validated(
                downloadUrl = url"http://example.com/test.xml",
                reportingPeriod = Year.of(2024),
                fileName = "test.xml",
                checksum = "checksum",
                size = 1337
              ),
              created = now,
              updated = now
            )
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a submitted state" - {

          "must redirect to the check file page" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Submitted("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }
        }

        "when the submission is in a rejected state" - {

          "must redirect to the file failed page" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Rejected("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.FileErrorsController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          }


          "must retrun future failed when called get subscription" in {

            val application = applicationBuilder(userAnswers = None)
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector),
                bind[PlatformOperatorConnector].toInstance(mockConnector),
                bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
              )
              .build()

            val submission = Submission(
              _id = "id",
              submissionType = SubmissionType.Xml,
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = operatorName,
              assumingOperatorName = None,
              state = Rejected("test.xml", Year.of(2024)),
              created = now,
              updated = now
            )
            when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.failed(new RuntimeException()))
            when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

            running(application) {
              val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
              val result = route(application, request).value.failed.futureValue
            }
            verify(mockSubscriptionConnector, times(1)).getSubscription(any())
            verify(mockConnector, times(0)).viewPlatformOperator(any())(any())
          }
        }

        "must retrun future failed when called view Platform operator" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            submissionType = SubmissionType.Xml,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = Rejected("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )
          when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
          when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.failed(new RuntimeException()))
          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value.failed.futureValue
          }
          verify(mockSubscriptionConnector, times(1)).getSubscription(any())
          verify(mockConnector, times(1)).viewPlatformOperator(any())(any())

        }
        
      }
    }
  }
}
