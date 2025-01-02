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

package models.subscription

import builders.IndividualContactBuilder.anIndividualContact
import builders.OrganisationContactBuilder.anOrganisationContact
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class SubscriptionInfoSpec extends AnyFreeSpec with Matchers {

  ".primaryContactName" - {
    "must return primary contact name when OrganisationContact" in {
      val underTest = aSubscriptionInfo.copy(primaryContact = anOrganisationContact.copy(organisation = Organisation("some-name")))

      underTest.primaryContactName mustBe "some-name"
    }

    "must return primary contact name when IndividualContact" in {
      val underTest = aSubscriptionInfo.copy(primaryContact = anIndividualContact.copy(individual = Individual("first-name", "last-name")))

      underTest.primaryContactName mustBe "first-name last-name"
    }
  }
}
