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

import config.Constants.viewSubmissionsPageSize
import controllers.submission.routes
import models.ViewSubmissionsFilter
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.submission.SortBy.{ReportingPeriod, SubmissionDate}
import models.submission.SortOrder.{Ascending, Descending}
import models.submission.SubmissionStatus.{Pending, Success}
import models.submission.{SortBy, SubmissionStatus, SubmissionSummary, SubmissionsSummary}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.http.StringContextOps

import java.net.URI
import java.time.*

class ViewSubmissionsViewModelSpec extends AnyFreeSpec with Matchers with OptionValues with GuiceOneAppPerSuite {

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
  private val thisYear = Year.now
  private implicit val msgs: Messages = stubMessages()
  private implicit lazy val request: Request[?] = FakeRequest(routes.ViewSubmissionsController.onPageLoad())

  ".apply" - {

    "must include a list of reporting period select items from the first legislative year to the current year" in {

      val expectedReportingPeriods = (2024 to thisYear.getValue).map(_.toString)

      val viewModel = ViewSubmissionsViewModel(None, Nil, defaultFilter, thisYear)
      val expectedValues = (expectedReportingPeriods :+ "0").sorted
      viewModel.reportingPeriodSelectItems.flatMap(_.value) must contain theSameElementsInOrderAs expectedValues
    }

    "must not include platform operator select items when there is only one platform operator" in {

      val viewModel = ViewSubmissionsViewModel(None, Seq(operator1), defaultFilter, thisYear)
      viewModel.platformOperatorSelectItems mustBe empty
    }

    "must include platform operator select items when there is more than one platform operator" in {

      val operator2 = operator1.copy(operatorId = "operatorId2")
      val viewModel = ViewSubmissionsViewModel(None, Seq(operator1, operator2), defaultFilter, thisYear)
      viewModel.platformOperatorSelectItems.flatMap(_.value) must contain theSameElementsInOrderAs Seq("all", "operatorId1", "operatorId2")
    }

    "pagination" - {

      val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
      val submissionSummaryWithTenPages = SubmissionsSummary(submissions, viewSubmissionsPageSize * 10, true, 0)

      s"must be None when there are $viewSubmissionsPageSize or fewer submissions" in {

        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, thisYear)
        viewModel.pagination must not be defined
      }

      "must highlight the current page" in {

        for (page <- 1 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          val currentItems = pagination.items.value.filter(_.current.contains(true))
          currentItems.size mustEqual 1
          currentItems.head.number.value mustEqual page.toString
        }
      }

      "must not include a `previous` link when the current page is 1" in {

        val filter = defaultFilter.copy(pageNumber = 1)
        val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        pagination.previous must not be defined
      }

      "must include a `previous` link on all pages except page 1" in {

        for (page <- 2 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          if (page == 2) checkQueryParameters(
            href = pagination.previous.value.href,
            expectedParameters = Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName)
          )
          if (page > 2)  checkQueryParameters(
            href = pagination.previous.value.href,
            expectedParameters = Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> (page - 1).toString)
          )
        }
      }

      "must not include a `next` link when the current page is the last page" in {

        val filter = defaultFilter.copy(pageNumber = 10)
        val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        pagination.next must not be defined
      }

      "must include a `next` link when the current page is not the last page" in {

        for (page <- 1 to 9) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          checkQueryParameters(
            href = pagination.next.value.href,
            expectedParameters = Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> (page + 1).toString)
          )
        }
      }

      "must start `1 ... x-1 [x]` when the current page is 4 or higher" in {

        for (page <- 4 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          val items = pagination.items.value
          items(0).number.value   mustEqual "1"
          items(1).ellipsis.value mustEqual true
          items(2).number.value   mustEqual (page - 1).toString
          items(3).number.value   mustEqual page.toString
          checkQueryParameters(items(0).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName))
          checkQueryParameters(items(2).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> (page - 1).toString))
          checkQueryParameters(items(3).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> page.toString))
        }
      }

      "must not include ellipses after page 1 when the current page is 3 or lower" in {

        for (page <- 1 to 3) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          val items = pagination.items.value
          items(0).number.value mustEqual "1"
          items(1).ellipsis must not be defined
        }
      }

      "must end `[x] x+1 ... (number of pages)` when the current page is (number of pages minus 3) or lower" in {

        for (page <- 1 to 7) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          val itemsReversed = pagination.items.value.reverse
          itemsReversed(0).number.value   mustEqual "10"
          itemsReversed(1).ellipsis.value mustEqual true
          itemsReversed(2).number.value   mustEqual (page + 1).toString
          itemsReversed(3).number.value   mustEqual page.toString

          checkQueryParameters(itemsReversed(0).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "10"))
          checkQueryParameters(itemsReversed(2).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> (page + 1).toString))
        }
      }

      "must not include ellipses before the last page when the current page is (number of pages minus 2) or higher" in {

        for (page <- 8 to 10) {
          val filter = defaultFilter.copy(pageNumber = page)
          val viewModel = ViewSubmissionsViewModel(Some(submissionSummaryWithTenPages), Nil, filter, thisYear)

          val pagination = viewModel.pagination.value
          val itemsReversed = pagination.items.value.reverse
          itemsReversed(0).number.value mustEqual "10"
          itemsReversed(1).ellipsis must not be defined
        }
      }

      "must include items for all pages when there are only 2 pages" in {

        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        items.size mustEqual 2
        items(0).number.value mustEqual "1"
        items(1).number.value mustEqual "2"
        checkQueryParameters(items(0).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName))
        checkQueryParameters(items(1).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "2"))
      }

      "must include items for all pages when there are 3 pages" in {

        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 3, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        items.size mustEqual 3
        items(0).number.value mustEqual "1"
        items(1).number.value mustEqual "2"
        items(2).number.value mustEqual "3"
        checkQueryParameters(items(0).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName))
        checkQueryParameters(items(1).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "2"))
        checkQueryParameters(items(2).href, Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "3"))
      }

      "must include reportingPeriod, operatorId and statuses in pagination links when they are in the filter" in {

        val filter = defaultFilter.copy(
          reportingPeriod = Some(Year.of(2024)),
          operatorId = Some("operatorId"),
          statuses = Set(Success, Pending)
        )
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value

        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set(
            "reportingPeriod" -> "2024",
            "operatorId"      -> "operatorId",
            "statuses[0]"     -> Success.entryName,
            "statuses[1]"     -> Pending.entryName,
            "sortBy"          -> SubmissionDate.entryName,
            "sortOrder"       -> Descending.entryName
          )
        )

        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set(
            "reportingPeriod" -> "2024",
            "operatorId"      -> "operatorId",
            "statuses[0]"     -> Success.entryName,
            "statuses[1]"     -> Pending.entryName,
            "page"            -> "2",
            "sortBy"          -> SubmissionDate.entryName,
            "sortOrder"       -> Descending.entryName
          )
        )
      }

      "must include `Ascending` in pagination links when it is in the filter" in {

        val filter = defaultFilter.copy(sortOrder = Ascending)
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set("sortOrder" -> Ascending.entryName, "sortBy" -> SubmissionDate.entryName)
        )
        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set("sortOrder" -> Ascending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "2")
        )
      }

      "must include `Descending` in the pagination links when it is in the filter" in {

        val filter = defaultFilter.copy(sortOrder = Descending)
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName)
        )
        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set("sortOrder" -> Descending.entryName, "sortBy" -> SubmissionDate.entryName, "page" -> "2")
        )
      }

      "must include `Reporting period` as the sort option when it is in the filter" in {

        val filter = defaultFilter.copy(sortBy = ReportingPeriod)
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set("sortBy" -> ReportingPeriod.entryName, "sortOrder" -> Descending.entryName)
        )
        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set("sortBy" -> ReportingPeriod.entryName, "sortOrder" -> Descending.entryName, "page" -> "2")
        )
      }

      "must include `platform operator` as the sort option when it is in the filter" in {

        val filter = defaultFilter.copy(sortBy = SortBy.PlatformOperator)
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set("sortBy" -> SortBy.PlatformOperator.entryName, "sortOrder" -> Descending.entryName)
        )
        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set("sortBy" -> SortBy.PlatformOperator.entryName, "sortOrder" -> Descending.entryName, "page" -> "2")
        )
      }

      "must include `Submission date` as the sort option when it is in the filter" in {

        val filter = defaultFilter.copy(sortBy = SubmissionDate)
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        val pagination = viewModel.pagination.value
        val items = pagination.items.value
        checkQueryParameters(
          href = items(0).href,
          expectedParameters = Set("sortBy" -> SubmissionDate.entryName, "sortOrder" -> Descending.entryName)
        )
        checkQueryParameters(
          href = items(1).href,
          expectedParameters = Set("sortBy" -> SubmissionDate.entryName, "sortOrder" -> Descending.entryName, "page"-> "2")
        )
      }
    }

    "submission date sort link must set sort order to Submission Date" - {

      "must include reporting period and statuses, but not page number, when they are in the filter" in {

        val filter = defaultFilter.copy(
          reportingPeriod = Some(Year.of(2024)),
          operatorId = Some("operatorId"),
          statuses = Set(Success, Pending),
          pageNumber = 2
        )
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        checkQueryParameters(viewModel.submissionDateSort.href, Set(
          "sortBy" -> SubmissionDate.entryName,
          "operatorId"      -> "operatorId",
          "sortOrder" -> Ascending.entryName,
          "reportingPeriod" -> "2024",
          "statuses[0]" -> Success.entryName,
          "statuses[1]" -> Pending.entryName
        ))
      }

      "must set sort order to Descending when the filter is currently sorted by submission date ascending" in {

        val filter = defaultFilter.copy(sortBy = SubmissionDate, sortOrder = Ascending)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.submissionDateSortIcon must equal ("\u25b2")

        checkQueryParameters(viewModel.submissionDateSort.href, Set(
          "sortBy" -> SubmissionDate.entryName,
          "sortOrder" -> Descending.entryName
        ))
      }

      "must set sort order to Descending when the filter is currently sorted by something other than submission date" in {

        val filter = defaultFilter.copy(sortBy = ReportingPeriod)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.submissionDateSortIcon must equal ("\u25bc\u25b2")

        checkQueryParameters(viewModel.submissionDateSort.href, Set(
          "sortBy" -> SubmissionDate.entryName,
          "sortOrder" -> Descending.entryName
        ))
      }

      "must set sort order to Ascending when the filter is currently sorted by submission date descending" in {

        val filter = defaultFilter.copy(sortBy = SubmissionDate, sortOrder = Descending)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.submissionDateSortIcon must equal("\u25bc")

        checkQueryParameters(viewModel.submissionDateSort.href, Set(
          "sortBy" -> SubmissionDate.entryName,
          "sortOrder" -> Ascending.entryName
        ))
      }
    }

    "reporting period sort link must set sort by to Reporting Period" - {

      "and must include reporting period and statuses, but not page number, when they are in the filter" in {

        val filter = defaultFilter.copy(
          reportingPeriod = Some(Year.of(2024)),
          operatorId = Some("operatorId"),
          statuses = Set(Success, Pending),
          pageNumber = 2
        )
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        checkQueryParameters(viewModel.reportingPeriodSort.href, Set(
          "sortBy" -> ReportingPeriod.entryName,
          "operatorId"      -> "operatorId",
          "sortOrder" -> Descending.entryName,
          "reportingPeriod" -> "2024",
          "statuses[0]" -> Success.entryName,
          "statuses[1]" -> Pending.entryName
        ))
      }

      "must set sort order to Descending when the filter is currently sorted by reporting period ascending" in {

        val filter = defaultFilter.copy(sortBy = ReportingPeriod, sortOrder = Ascending)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.reportingPeriodSortIcon must equal("\u25b2")

        checkQueryParameters(viewModel.reportingPeriodSort.href, Set(
          "sortBy" -> ReportingPeriod.entryName,
          "sortOrder" -> Descending.entryName
        ))
      }

      "must set sort order to Descending when the filter is currently sorted by something other than reporting period" in {

        val filter = defaultFilter.copy(sortBy = SubmissionDate, sortOrder = Descending)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.reportingPeriodSortIcon must equal("\u25bc\u25b2")

        checkQueryParameters(viewModel.reportingPeriodSort.href, Set(
          "sortBy" -> ReportingPeriod.entryName,
          "sortOrder" -> Descending.entryName
        ))
      }

      "must set sort order to Ascending when the filter is currently sorted by reporting period descending" in {

        val filter = defaultFilter.copy(sortBy = ReportingPeriod, sortOrder = Descending)
        val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
        val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize * 2, true, 0)

        val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, filter, thisYear)

        viewModel.reportingPeriodSortIcon must equal("\u25bc")

        checkQueryParameters(viewModel.reportingPeriodSort.href, Set(
          "sortBy" -> ReportingPeriod.entryName,
          "sortOrder" -> Ascending.entryName
        ))
      }
    }
  }

  "must not include page number information in the title when there are no submissions" in {

    val viewModel = ViewSubmissionsViewModel(None, Nil, defaultFilter, thisYear)
    viewModel.pageTitle mustEqual msgs("viewSubmissions.title")
  }

  s"must not include page number information in the title when there are $viewSubmissionsPageSize submissions or fewer" in {

    val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
    val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize, true, 0)
    val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, thisYear)
    viewModel.pageTitle mustEqual msgs("viewSubmissions.title")
  }

  s"must include page number information in the title when there are ${viewSubmissionsPageSize + 1} submissions or more" in {

    val submissions = (1 to viewSubmissionsPageSize).map(i => submission.copy(submissionId = i.toString))
    val submissionsSummary = SubmissionsSummary(submissions, viewSubmissionsPageSize + 1, true, 0)
    val viewModel = ViewSubmissionsViewModel(Some(submissionsSummary), Nil, defaultFilter, thisYear)
    viewModel.pageTitle mustEqual msgs("viewSubmissions.title.pages", 1, 2)
  }

  private def checkQueryParameters(href: String, expectedParameters: Set[(String, String)]): Unit = {

    val query = URI(href).getQuery
    if (query == null) {
      expectedParameters mustBe empty
    } else {
      val queryElements: Set[List[String]] = query.split('&').toList.toSet.map(_.split('=').toList)

      queryElements must contain theSameElementsAs expectedParameters.map((a, b) => List(a, b))
    }
  }
}
