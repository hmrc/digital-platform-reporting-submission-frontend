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

package config

import controllers.actions.*
import models.CountriesList
import play.api.inject.Binding
import play.api.{Configuration, Environment}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.time.Clock

class Module extends play.api.inject.Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[?]] = {
    Seq(
      bind[DataRequiredAction].to[DataRequiredActionImpl].eagerly(),
      bind[IdentifierAction].to[AuthenticatedIdentifierAction].eagerly(),
      bind[XmlSubmissionSentCheckAction].to[XmlSubmissionSentCheckActionImpl],
      bind[AssumedSubmissionSentCheckAction].to[AssumedSubmissionSentCheckActionImpl],
      bind[AssumedSubmissionDeletedCheckAction].to[AssumedSubmissionDeletedCheckActionImpl],
      bind[Clock].toInstance(Clock.systemUTC()),
      bind[Encrypter & Decrypter].toProvider[CryptoProvider],
      bind[CountriesList].toProvider[CountriesListProvider]
    )
  }
}
