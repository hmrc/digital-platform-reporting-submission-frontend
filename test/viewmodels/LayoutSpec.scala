/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*
import scala.util.Using

class LayoutSpec extends AnyWordSpec with Matchers {

  private val viewsDirectory: Path =
    Paths.get("app", "/views")

  private val h1Pattern =
    """(?s)<h1\b[^>]*>""".r

  private def allPages: Seq[Path] =
    Using.resource(Files.walk(viewsDirectory)) { paths =>
      paths
        .iterator()
        .asScala
        .filter(_.toString.endsWith(".scala.html"))
        .filterNot(_.getFileName.toString == "SubmissionConfirmationView.scala.html")
        .toSeq
    }

  "All h1 elements" should {

    "use govuk-heading-l and not govuk-heading-xl" in {
      val invalidHeadings =
        allPages.flatMap { file =>
          val source = Files.readString(file)

          h1Pattern
            .findAllIn(source)
            .filterNot { h1 =>
              h1.contains("govuk-heading-l") &&
                !h1.contains("govuk-heading-xl")
            }
            .map(h1 => s"$file: $h1")
        }

      assert(
        invalidHeadings.isEmpty,
        s"""Some h1 elements do not use govuk-heading-l:
           |${invalidHeadings.mkString("\n")}
           |""".stripMargin
      )
    }
  }
}
