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

package models.operator

import enumeratum.*

sealed abstract class TinType(override val entryName: String) extends EnumEntry

object TinType extends PlayEnum[TinType] {

  override val values: IndexedSeq[TinType] = findValues

  case object Crn extends TinType("CRN")
  case object Utr extends TinType("UTR")
  case object Vrn extends TinType("VRN")
  case object Empref extends TinType("EMPREF")
  case object Chrn extends TinType("CHRN")
  case object Other extends TinType("OTHER")
}
