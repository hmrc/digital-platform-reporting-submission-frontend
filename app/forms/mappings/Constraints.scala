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

package forms.mappings

import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

trait Constraints {

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint { input =>
      constraints
        .map(_.apply(input))
        .find(_ != Valid)
        .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String, args: Any*)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>
        import ev.*

        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, minimum +: args *)
        }
    }

  protected def maximumValue[A](maximum: A, errorKey: String, args: Any*)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>
        import ev.*

        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, maximum +: args *)
        }
    }

  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>
        import ev.*

        if (input >= minimum && input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, minimum, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String, args: Any*): Constraint[String] =
    Constraint {
      case str if str.matches(regex) => Valid
      case _ => Invalid(errorKey, regex +: args *)
    }

  protected def maxLength(maximum: Int, errorKey: String, args: Any*): Constraint[String] =
    Constraint {
      case str if str.length <= maximum => Valid
      case _ => Invalid(errorKey, maximum +: args *)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) => Invalid(errorKey, args *)
      case _ => Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) => Invalid(errorKey, args *)
      case _ => Valid
    }

  protected def nonEmptySet(errorKey: String, args: Any*): Constraint[Set[?]] =
    Constraint {
      case set if set.nonEmpty => Valid
      case _ => Invalid(errorKey, args *)
    }
}
