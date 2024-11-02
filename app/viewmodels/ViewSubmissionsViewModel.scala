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

import config.Constants.firstLegislativeYear
import models.ViewSubmissionsFilter
import models.operator.responses.PlatformOperator
import models.submission.{SubmissionSummary, SubmissionsSummary}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

import java.time.{Clock, Year}

final case class ViewSubmissionsViewModel(
                                           maybeSummary: Option[SubmissionsSummary],
                                           reportingPeriodSelectItems: Seq[SelectItem],
                                           platformOperatorSelectItems: Seq[SelectItem],
                                           pagination: Option[Pagination],
                                           filter: ViewSubmissionsFilter,
                                           recordCountInfo: Option[String]
                                         )

object ViewSubmissionsViewModel {
  
  def apply(
             maybeSummary: Option[SubmissionsSummary],
             operators: Seq[PlatformOperator],
             filter: ViewSubmissionsFilter,
             clock: Clock
           )(implicit messages: Messages): ViewSubmissionsViewModel =
    ViewSubmissionsViewModel(
      maybeSummary,
      reportingPeriodSelectItems(clock),
      platformOperatorSelectItems(operators),
      maybeSummary.flatMap(summary => pagination(summary.deliveredSubmissionRecordCount, filter.pageNumber)),
      filter,
      maybeSummary.flatMap(summary => getRecordCountInfo(summary.deliveredSubmissionRecordCount, filter.pageNumber))
    )
  
  private def getRecordCountInfo(numberOfSubmissions: Int, pageNumber: Int)(implicit messages: Messages): Option[String] =
    if (numberOfSubmissions > 0) {
      val firstRecord = 1 + (pageNumber - 1) * 10
      val lastRecord  = (pageNumber * 10).min(numberOfSubmissions)
      
      Some(messages("viewSubmissions.recordCountInfo", firstRecord, lastRecord, numberOfSubmissions))
    } else {
      None
    }
  
  private def reportingPeriodSelectItems(clock: Clock): Seq[SelectItem] = {
    SelectItem() ::
      (firstLegislativeYear to Year.now(clock).getValue).map { year =>
        SelectItem(
          value = Some(year.toString),
          text  = year.toString
        )
      }.toList
  }

  private def platformOperatorSelectItems(operators: Seq[PlatformOperator]): List[SelectItem] =
    if (operators.size > 1) {
      SelectItem() ::
        operators.map { operator =>
          SelectItem(
            value = Some(operator.operatorId),
            text  = operator.operatorName
          )
        }.toList
    } else {
      Nil
    }
    
  private def pagination(numberOfSubmissions: Int, pageNumber: Int)(implicit messages: Messages): Option[Pagination] = {
    if (numberOfSubmissions > 10) {
      val numberOfPages = (numberOfSubmissions + 9) / 10
      
      val items =
        paginationStart(pageNumber) ++
        paginationCurrentSection(pageNumber, numberOfPages) ++
        paginationEnd(pageNumber, numberOfPages)

      val nextLink     = if (pageNumber < numberOfPages) Some(PaginationLink(href = "")) else None
      val previousLink = if(pageNumber > 1)              Some(PaginationLink(href = "")) else None
      
      Some(Pagination(items = Some(items), previous = previousLink, next = nextLink))
    } else {
      None
    }
  }
  
  private def paginationStart(pageNumber: Int) = pageNumber match {
    case x if x <= 2 => Nil
    case 3           => Seq(PaginationItem(href = "", number = Some("1")))
    case _           => Seq(PaginationItem(href = "", number = Some("1")), PaginationItem(ellipsis = Some(true)))
  }
  
  private def paginationEnd(pageNumber: Int, numberOfPages: Int) = numberOfPages - pageNumber match {
    case x if x <= 1 => Nil
    case 2           => Seq(PaginationItem(href = "", number = Some(numberOfPages.toString)))
    case _           => Seq(PaginationItem(ellipsis = Some(true)), PaginationItem(href = "", number = Some(numberOfPages.toString)))
  }

  private def paginationCurrentSection(pageNumber: Int, numberOfPages: Int) = {

    val currentPage  = Some(PaginationItem(href = "", number = Some(pageNumber.toString), current = Some(true)))
    val previousPage = if (pageNumber > 1)             Some(PaginationItem(href = "", number = Some((pageNumber - 1).toString))) else None
    val nextPage     = if (pageNumber < numberOfPages) Some(PaginationItem(href = "", number = Some((pageNumber + 1).toString))) else None

    Seq(previousPage, currentPage, nextPage).flatten
  }
}
