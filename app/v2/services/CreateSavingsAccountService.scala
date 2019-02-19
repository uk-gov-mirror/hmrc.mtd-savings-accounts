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

package v2.services

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.CreateSavingsAccountRequestData

import scala.concurrent.{ExecutionContext, Future}

class CreateSavingsAccountService @Inject()(connector: DesConnector) {

  val logger: Logger = Logger(this.getClass)

  def create(createSavingsAccountRequestData: CreateSavingsAccountRequestData)
            (implicit hc: HeaderCarrier,
             ec: ExecutionContext): Future[CreateSavingsAccountOutcome] = {
    connector.create(createSavingsAccountRequestData).map {
      case Left(DesResponse(correlationId, MultipleErrors(errors))) =>
        val mtdErrors = errors.map(error => desErrorToMtdError(error.code))
        if(mtdErrors.contains(DownstreamError)) {
          logger.info(s"[CreateSavingsAccountService] [create] [CorrelationId - $correlationId]" +
            s" - downstream returned INVALID_IDTYPE, SERVER_ERROR or SERVICE_UNAVAILABLE. Revert to ISE")
          Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
        } else {
          Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(mtdErrors)))
        }
      case Left(DesResponse(correlationId, SingleError(error))) => Left(ErrorWrapper(Some(correlationId), desErrorToMtdError(error.code), None))
      case Left(DesResponse(correlationId, OutboundError(error))) => Left(ErrorWrapper(Some(correlationId), error, None))
      case Right(desResponse) => Right(DesResponse(desResponse.correlationId, desResponse.responseData))
    }
  }

  private def desErrorToMtdError = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "MAX_ACCOUNTS_REACHED" -> MaximumSavingsAccountsLimitError,
    "ALREADY_EXISTS" -> AccountNameDuplicateError,
    "INVALID_PAYLOAD" -> BadRequestError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  )

}