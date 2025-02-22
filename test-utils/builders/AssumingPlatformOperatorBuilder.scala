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

package builders

import builders.TinDetailsBuilder.aTinDetails
import models.Country
import models.submission.AssumingPlatformOperator

object AssumingPlatformOperatorBuilder {

  val anAssumingPlatformOperator: AssumingPlatformOperator = AssumingPlatformOperator(
    name = "default-assuming-platform-operator-name",
    residentCountry = Country.UnitedKingdom,
    tinDetails = Seq(aTinDetails),
    registeredCountry = Country.UnitedKingdom,
    address = "default-address",
  )
}
