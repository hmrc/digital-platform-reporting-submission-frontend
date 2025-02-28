/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import base.SpecBase
import connectors.ConfirmedDetailsConnector.{
    GetConfirmedBusinessDetailsFailure,
    SaveConfirmedBusinessDetailsFailure,
    GetConfirmedReportingNotificationsFailure,
    SaveConfirmedReportingNotificationsFailure,
    GetConfirmedContactDetailsFailure,
    SaveConfirmedContactDetailsFailure
  }

    class ConfirmedDetailsConnectorSpec extends SpecBase {
      private val status = 503
      "GetConfirmedBusinessDetailsFailure" - {
        "must contain correct message" in {
          val test = GetConfirmedBusinessDetailsFailure(status)
          test.getMessage mustBe s"Get confirmed business details failed with status: $status"
        }
      }
  
      "SaveConfirmedBusinessDetailsFailure" - {
        "must contain correct message" in {
          val test = SaveConfirmedBusinessDetailsFailure(status)
          test.getMessage mustBe s"Save confirmed business details failed with status: $status"
        }
      }
  
      "GetConfirmedReportingNotificationsFailure" - {
        "must contain correct message" in {
          val test = GetConfirmedReportingNotificationsFailure(status)
          test.getMessage mustBe s"Get confirmed reporting notifications failed with status: $status"
        }
      }
      
      "SaveConfirmedReportingNotificationsFailure" - {
        "must contain correct message" in {
          val test = SaveConfirmedReportingNotificationsFailure(status)
          test.getMessage mustBe s"Save confirmed reporting notifications failed with status: $status"
        }
      }
      
      "GetConfirmedContactDetailsFailure" - {
        "must contain correct message" in {
          val test = GetConfirmedContactDetailsFailure(status)
          test.getMessage mustBe s"Get confirmed contact details failed with status: $status"
        }
      }
      
      "SaveConfirmedContactDetailsFailure" - {
        "must contain correct message" in {
          val test = SaveConfirmedContactDetailsFailure(status)
          test.getMessage mustBe s"Save confirmed contact details failed with status: $status"
        }
      }
  
  
  
}
