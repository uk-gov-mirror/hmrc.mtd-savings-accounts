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

package v2.controllers.requestParsers.validators

import v2.models.errors.Error
import v2.models.requestData.InputData

trait Validator[A <: InputData] {


  type ValidationLevel[T] = T => List[Error]

  def validate(data: A): List[Error]

  def run(validationSet: List[A => List[List[Error]]], data: A): List[Error] = {

    validationSet match {
      case Nil => List()
      case thisLevel :: remainingLevels => thisLevel(data).flatten match {
        case x if x.isEmpty => run(remainingLevels, data)
        case x if x.nonEmpty => x
      }
    }
  }
}