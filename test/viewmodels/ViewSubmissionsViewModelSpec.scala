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

package viewmodels

import models.ViewSubmissionsFilter
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.submission.SortBy.SubmissionDate
import models.submission.SortOrder.Descending
import models.submission.{SubmissionStatus, SubmissionSummary, SubmissionsSummary}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.*

class ViewSubmissionsViewModelSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val operator1 = PlatformOperator(
    operatorId = "operatorId1",
    operatorName = "operatorName",
    tinDetails = Nil,
    businessName = None,
    tradingName = None,
    primaryContactDetails = ContactDetails(None, "name", "email"),
    secondaryContactDetails = None,
    addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
    notifications = Nil
  )

  private val submission = SubmissionSummary(
    submissionId = "id1",
    fileName = "file",
    operatorId = "operatorId",
    operatorName = "operatorName",
    reportingPeriod = Year.of(2024),
    submissionDateTime = now,
    submissionStatus = SubmissionStatus.Success,
    assumingReporterName = None,
    submissionCaseId = None
  )
  
  private val defaultFilter = ViewSubmissionsFilter(
    pageNumber = 1,
    sortBy = SubmissionDate,
    sortOrder = Descending,
    statuses = Set.empty,
    operatorId = None,
    reportingPeriod = None
  )

  private val now = Instant.now
  private val stubClock = Clock.fixed(now, ZoneId.systemDefault)
  private val today = LocalDate.now(stubClock)
  private implicit val msgs: Messages = stubMessages()

  ".apply" - {

    "must include a list of reporting period select items from the first legislative year to the current year" in {

      val expectedReportingPeriods = (2024 to today.getYear).map(_.toString)
      
      val viewModel = ViewSubmissionsViewModel(None, Nil, defaultFilter, stubClock)
      viewModel.reportingPeriodSelectItems.flatMap(_.value) must contain theSameElementsInOrderAs expectedReportingPeriods
    }

    "must not include platform operator select items when there is only one platform operator" in {

      val viewModel = ViewSubmissionsViewModel(None, Seq(operator1), defaultFilter, stubClock)
      viewModel.platformOperatorSelectItems mustBe empty
    }

    "must include platform operator select items when there is more than one platform operator" in {

      val operator2 = operator1.copy(operatorId = "operatorId2")
      val viewModel = ViewSubmissionsViewModel(None, Seq(operator1, operator2), defaultFilter, stubClock)
      viewModel.platformOperatorSelectItems.flatMap(_.value) must contain theSameElementsInOrderAs Seq("operatorId1", "operatorId2")
    }

    "pagination" - {

      val submissions = (1 to 10).map(i => submission.copy(submissionId = i.toString))
      val oneHundredSubmissionSummary = SubmissionsSummary(submissions, Nil, 100)

      "must be None when there are 10 or fewer submissions" in {

        val submissionsSummary = SubmissionsSummary(submissions, Nil, 10)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, stubClock)
        viewModel.pagination must not be defined
      }

      "must highlight the current page" in {

        for (page <- 1 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)
          
          val pagination = viewModel.pagination.value
          val currentItems = pagination.items.value.filter(_.current.contains(true))
          currentItems.size mustEqual 1
          currentItems.head.number.value mustEqual page.toString
        }
      }
      
      "must not include a `previous` link when the current page is 1" in {

        val filter = defaultFilter.copy(pageNumber = 1)
        val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

        val pagination = viewModel.pagination.value
        pagination.previous must not be defined
      }
      
      "must include a `previous` link on all pages except page 1" in {

        for (page <- 2 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          pagination.previous mustBe defined
        }
      }
      
      "must not include a `next` link when the current page is the last page" in {

        val filter = defaultFilter.copy(pageNumber = 10)
        val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

        val pagination = viewModel.pagination.value
        pagination.next must not be defined
      }
      
      "must include a `next` link when the current page is not the last page" in {
        
        for (page <- 1 to 9) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          pagination.next mustBe defined
        }
      }

      "must start `1 ... x-1 [x]` when the current page is 4 or higher" in {

        for (page <- 4 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          val items = pagination.items.value
          items(0).number.value mustEqual "1"
          items(1).ellipsis.value mustEqual true
          items(2).number.value mustEqual (page - 1).toString
          items(3).number.value mustEqual page.toString
        }
      }

      "must not include ellipses after page 1 when the current page is 3 or lower" in {

        for (page <- 1 to 3) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          val items = pagination.items.value
          items(0).number.value mustEqual "1"
          items(1).ellipsis must not be defined
        }
      }

      "must end `[x] x+1 ... (number of pages)` when the current page is (number of pages minus 3) or lower" in {

        for (page <- 1 to 7) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          val itemsReversed = pagination.items.value.reverse
          itemsReversed(0).number.value mustEqual "10"
          itemsReversed(1).ellipsis.value mustEqual true
          itemsReversed(2).number.value mustEqual (page + 1).toString
          itemsReversed(3).number.value mustEqual page.toString
        }
      }

      "must not include ellipses before the last page when the current page is (number of pages minus 2) or higher" in {

        for (page <- 8 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(oneHundredSubmissionSummary), Nil, filter, stubClock)

          val pagination = viewModel.pagination.value
          val itemsReversed = pagination.items.value.reverse
          itemsReversed(0).number.value mustEqual "10"
          itemsReversed(1).ellipsis must not be defined
        }
      }

      "must include links for all pages when there are only 2 pages" in {
        
        val submissionsSummary = SubmissionsSummary(submissions, Nil, 20)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, stubClock)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        items.size mustEqual 2
        items(0).number.value mustEqual "1"
        items(1).number.value mustEqual "2"
      }

      "must include links for all pages when there are 3 pages" in {

        val submissionsSummary = SubmissionsSummary(submissions, Nil, 30)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, stubClock)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        items.size mustEqual 3
        items(0).number.value mustEqual "1"
        items(1).number.value mustEqual "2"
        items(2).number.value mustEqual "3"
      }
    }
  }
}
