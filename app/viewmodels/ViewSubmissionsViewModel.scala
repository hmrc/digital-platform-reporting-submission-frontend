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

import config.Constants.{firstLegislativeYear, viewSubmissionsPageSize}
import controllers.submission.routes
import models.ViewSubmissionsFilter
import models.operator.responses.PlatformOperator
import models.submission.SortBy.{ReportingPeriod, SubmissionDate}
import models.submission.SortOrder.{Ascending, Descending}
import models.submission.{SortBy, SubmissionsSummary}
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import uk.gov.hmrc.http.StringContextOps

import java.time.Year

final case class ViewSubmissionsViewModel(
                                           maybeSummary: Option[SubmissionsSummary],
                                           reportingPeriodSelectItems: Seq[SelectItem],
                                           platformOperatorSelectItems: Seq[SelectItem],
                                           pagination: Option[Pagination],
                                           filter: ViewSubmissionsFilter,
                                           recordCountInfo: Option[String],
                                           submissionDateSort: Link,
                                           submissionDateSortIcon: String,
                                           reportingPeriodSort: Link,
                                           reportingPeriodSortIcon: String,
                                           pageTitle: String
                                         )

object ViewSubmissionsViewModel {

  def apply(
             maybeSummary: Option[SubmissionsSummary],
             operators: Seq[PlatformOperator],
             filter: ViewSubmissionsFilter,
             currentYear: Year
           )(implicit request: Request[?], messages: Messages): ViewSubmissionsViewModel =
    ViewSubmissionsViewModel(
      maybeSummary,
      reportingPeriodSelectItems(currentYear),
      platformOperatorSelectItems(operators),
      maybeSummary.flatMap(summary => pagination(summary.deliveredSubmissionRecordCount, filter)),
      filter,
      maybeSummary.flatMap(summary => getRecordCountInfo(summary.deliveredSubmissionRecordCount, filter.pageNumber)),
      Link(messages("viewSubmissions.submissionDate"),submissionDateSortLink(filter)),
      sortingIconFor(filter, SubmissionDate),
      Link(messages("viewSubmissions.reportingPeriod"),reportingPeriodSortLink(filter)),
      sortingIconFor(filter, ReportingPeriod),
      getTitle(maybeSummary.map(_.deliveredSubmissionRecordCount).getOrElse(0), filter.pageNumber)
    )

  private def getRecordCountInfo(numberOfSubmissions: Int, pageNumber: Int)
                                (implicit messages: Messages): Option[String] =
    if (numberOfSubmissions > 0) {
      val firstRecord = 1 + (pageNumber - 1) * viewSubmissionsPageSize
      val lastRecord  = (pageNumber * viewSubmissionsPageSize).min(numberOfSubmissions)

      Some(messages("viewSubmissions.recordCountInfo", firstRecord, lastRecord, numberOfSubmissions))
    } else {
      None
    }
    
  private def getTitle(numberOfSubmissions: Int, pageNumber: Int)
                      (implicit messages: Messages): String =
    if (numberOfSubmissions < viewSubmissionsPageSize + 1) {
      messages("viewSubmissions.title")
    } else {
      messages("viewSubmissions.title.pages", pageNumber, getNumberOfPages(numberOfSubmissions))
    }

  private def reportingPeriodSelectItems(currentYear: Year)
                                        (implicit messages: Messages): Seq[SelectItem] = {
    SelectItem(value = Some("0"), text = messages("viewSubmissions.reportingPeriod.allValues")) ::
      (firstLegislativeYear to currentYear.getValue).map { year =>
        SelectItem(
          value = Some(year.toString),
          text  = year.toString
        )
      }.toList
  }

  private def platformOperatorSelectItems(operators: Seq[PlatformOperator])
                                         (implicit messages: Messages): List[SelectItem] =
    if (operators.size > 1) {
      SelectItem(value = Some("all"), text = messages("viewSubmissions.platformOperator.allValues")) ::
        operators.map { operator =>
          SelectItem(
            value = Some(operator.operatorId),
            text  = operator.operatorName
          )
        }.toList
    } else {
      Nil
    }

  private def getNumberOfPages(numberOfSubmissions: Int): Int =
    (numberOfSubmissions + (viewSubmissionsPageSize - 1)) / viewSubmissionsPageSize

  private def pagination(numberOfSubmissions: Int, filter: ViewSubmissionsFilter)
                        (implicit request: Request[?]): Option[Pagination] =
    if (numberOfSubmissions > viewSubmissionsPageSize) {
      val numberOfPages = getNumberOfPages(numberOfSubmissions)

      val items =
        paginationStart(filter) ++
        paginationCurrentSection(filter, numberOfPages) ++
        paginationEnd(filter, numberOfPages)

      val nextLink     = if (filter.pageNumber < numberOfPages) Some(PaginationLink(href = paginationHref(filter, filter.pageNumber + 1))) else None
      val previousLink = if (filter.pageNumber > 1)             Some(PaginationLink(href = paginationHref(filter, filter.pageNumber - 1))) else None

      Some(Pagination(items = Some(items), previous = previousLink, next = nextLink))
    } else {
      None
    }

  private def paginationStart(filter: ViewSubmissionsFilter)
                             (implicit request: Request[?]) =
    filter.pageNumber match {
      case x if x <= 2 => Nil
      case 3           => Seq(PaginationItem(href = paginationHref(filter, 1), number = Some("1")))
      case _           => Seq(PaginationItem(href = paginationHref(filter, 1), number = Some("1")), PaginationItem(ellipsis = Some(true)))
    }

  private def paginationEnd(filter: ViewSubmissionsFilter, numberOfPages: Int)
                           (implicit request: Request[?]) =
    numberOfPages - filter.pageNumber match {
      case x if x <= 1 => Nil
      case 2           => Seq(PaginationItem(href = paginationHref(filter, numberOfPages), number = Some(numberOfPages.toString)))
      case _           => Seq(
        PaginationItem(ellipsis = Some(true)),
        PaginationItem(href = paginationHref(filter, numberOfPages), number = Some(numberOfPages.toString))
      )
    }

  private def paginationCurrentSection(filter: ViewSubmissionsFilter, numberOfPages: Int)
                                      (implicit request: Request[?]) = {

    val currentPage  = Some(PaginationItem(
      href    = paginationHref(filter, filter.pageNumber),
      number  = Some(filter.pageNumber.toString),
      current = Some(true)
    ))

    val previousPage = if (filter.pageNumber > 1) {
      Some(PaginationItem(
        href   = paginationHref(filter, filter.pageNumber - 1),
        number = Some((filter.pageNumber - 1).toString)
      ))
    } else {
      None
    }

    val nextPage = if (filter.pageNumber < numberOfPages) {
      Some(PaginationItem(
        href   = paginationHref(filter, filter.pageNumber + 1),
        number = Some((filter.pageNumber + 1).toString)
      ))
    } else {
      None
    }

    Seq(previousPage, currentPage, nextPage).flatten
  }

  private def paginationHref(filter: ViewSubmissionsFilter, pageNumber: Int)
                            (implicit request: Request[?]): String = {
    val sortQueryParameters = Map(
      "sortBy" -> filter.sortBy.entryName,
      "sortOrder" -> filter.sortOrder.entryName
    )

    val queryParameters: Map[String, String] =
      filterQueryParameters(filter) ++
        sortQueryParameters ++
        pageNumberQueryParameter(pageNumber)

    buildLink(queryParameters)
  }

  private def filterQueryParameters(filter: ViewSubmissionsFilter): Map[String, String] = {
    val reportingPeriodQueryParameter = filter.reportingPeriod.map(period => Map("reportingPeriod" -> period.toString))
    val operatorIdQueryParameter      = filter.operatorId.map(operatorId => Map("operatorId" -> operatorId))
    val statusesQueryParameter        = filter.statuses.zipWithIndex.map((status, index) => s"statuses[$index]" -> status.entryName).toMap

    statusesQueryParameter ++
    operatorIdQueryParameter.getOrElse(Map.empty[String, String]) ++
      reportingPeriodQueryParameter.getOrElse(Map.empty[String, String])
  }

  private def pageNumberQueryParameter(pageNumber: Int): Map[String, String] =
    if (pageNumber > 1) Map("page" -> pageNumber.toString) else Map.empty[String, String]

  private def submissionDateSortLink(filter: ViewSubmissionsFilter)
                                    (implicit request: Request[?]): String = {
    val sortByQueryParameter = Map("sortBy" -> SubmissionDate.entryName)
    val sortOrderQueryParameter = if (filter.sortBy == SubmissionDate && filter.sortOrder == Descending) {
      Map("sortOrder" -> Ascending.entryName)
    } else {
      Map("sortOrder" -> Descending.entryName)
    }

    val queryParameters =
      filterQueryParameters(filter) ++
        sortOrderQueryParameter ++
        sortByQueryParameter

    buildLink(queryParameters)
  }

  private def reportingPeriodSortLink(filter: ViewSubmissionsFilter)
                                     (implicit request: Request[?]): String = {
    val sortByQueryParameter = Map("sortBy" -> ReportingPeriod.entryName)
    val sortOrderQueryParameter = if (filter.sortBy == ReportingPeriod && filter.sortOrder == Descending) {
      Map("sortOrder" -> Ascending.entryName)
    } else {
      Map("sortOrder" -> Descending.entryName)
    }

    val queryParameters =
      filterQueryParameters(filter) ++
        sortOrderQueryParameter ++
        sortByQueryParameter

    buildLink(queryParameters)
  }

  private def buildLink(queryParameters: Map[String, String])
                       (implicit request: Request[?]): String =
    if (queryParameters.isEmpty) {
      url"${routes.ViewSubmissionsController.onPageLoad().absoluteURL()}".toString
    } else {
      url"${routes.ViewSubmissionsController.onPageLoad().absoluteURL()}?$queryParameters".toString
    }

  private def sortingIconFor(filter: ViewSubmissionsFilter, sortBy: SortBy): String = {
    if (filter.sortBy.entryName.equals(sortBy.entryName)) {
      filter.sortOrder.entryName match {
        case "ASC" => "\u25b2"
        case "DSC" => "\u25bc"
      }
    } else {
      "\u25bc\u25b2"
    }
  }
}
