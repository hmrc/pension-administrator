/*
 * Copyright 2019 HM Revenue & Customs
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

package utils

import play.api.Logger
import play.api.libs.json.{JsLookupResult, JsResultException, JsValue, Reads}

package object validationUtils {

  implicit class genResponse(jsValue: JsValue) {
    implicit def convertTo[A](implicit rds: Reads[A]): A = {
      jsValue.validate[A].fold(
        invalid = {
          errors =>
            Logger.warn(s"Json contains bad data $errors")
            throw JsResultException(errors)
        },
        valid = { response =>
          response
        }
      )
    }
  }

  implicit class genOptResponse(jsLookupResult: JsLookupResult) {

    implicit def convertAsOpt[A](implicit rds: Reads[A]): Option[A] = {
      jsLookupResult.validateOpt[A].fold(
        invalid = {
          errors =>
            Logger.warn(s"Json look up contains bad data $errors")
            throw JsResultException(errors)
        },
        valid = { response =>
          response
        }
      )
    }

    implicit def convertTo[A](implicit rds: Reads[A]): A = {
      jsLookupResult.validate[A].fold(
        invalid = {
          errors =>
            Logger.warn(s"Json contains bad data $errors")
            throw JsResultException(errors)
        },
        valid = { response =>
          response
        }
      )
    }
  }

}
