/*
 * Copyright 2019 Frugal Mechanic (http://frugalmechanic.com)
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
package fm.json

import com.fasterxml.jackson.core.{JsonParseException, JsonParser, JsonToken}
import fm.common.Implicits._
import java.io.Reader
import scala.util.Try

trait JsonNodeParseFactory[A, N <: JsonNode] {
  def apply(value: A): N

  /**
   * Note: This should not advance to the next token
   */
  protected def parseImpl(parser: JsonParser, options: JsonOptions): A

  final def tryParse(s: String): Option[A] = tryParse(s, JsonOptions.default)
  final def tryParse(s: String, options: JsonOptions): Option[A] = Try{ parse(s, options) }.toOption

  final def tryParse(r: Reader): Option[A] = tryParse(r, JsonOptions.default)
  final def tryParse(r: Reader, options: JsonOptions): Option[A] = Try{ parse(r, options) }.toOption

  final def tryParse(parser: JsonParser): Option[A] = tryParse(parser, JsonOptions.default)
  final def tryParse(parser: JsonParser, options: JsonOptions): Option[A] = Try{ parse(parser, options) }.toOption

  final def parse(s: String): A = parse(s, JsonOptions.default)
  final def parse(s: String, options: JsonOptions): A = parse(Json.jsonFactory.createParser(s), options)

  final def parse(r: Reader): A = parse(r, JsonOptions.default)
  final def parse(r: Reader, options: JsonOptions): A = parse(Json.jsonFactory.createParser(r), options)

  final def parse(parser: JsonParser): A = parse(parser, JsonOptions.default)

  final def parse(parser: JsonParser, options: JsonOptions): A = {
    // This wraps the parseImpl method and makes sure that we clear the current token
    val res: A = parseImpl(parser, options)

    // Clear the token (which is idempotent unlike parser.nextToken())
    parser.clearCurrentToken()

    res
  }

  final def tryParseNode(s: String): Option[N] = tryParse(s).map{ apply }
  final def tryParseNode(r: Reader): Option[N] = tryParse(r).map{ apply }
  final def tryParseNode(parser: JsonParser): Option[N] = tryParse(parser).map{ apply }

  final def parseNode(s: String): N = apply(parse(s))
  final def parseNode(r: Reader): N = apply(parse(r))
  final def parseNode(parser: JsonParser): N = parseNode(parser, JsonOptions.default)
  final def parseNode(parser: JsonParser, options: JsonOptions): N = apply(parse(parser, options))

  final protected def currentTokenOrAdvance(parser: JsonParser): JsonToken = {
    if (!hasTokenOrAdvance(parser)) throw new JsonParseException(parser, "Incomplete JSON?")
    parser.currentToken()
  }

  final protected def hasTokenOrAdvance(parser: JsonParser, token: JsonToken): Boolean = {
    currentTokenOrAdvance(parser) === token
  }

  final protected def hasTokenOrAdvance(parser: JsonParser): Boolean = {
    parser.hasCurrentToken || parser.nextToken != null
  }

  final protected def requireToken(parser: JsonParser, expected: JsonToken): Unit = {
    requireParser(parser, parser.hasToken(expected), s"Expected $expected but got ${parser.getCurrentToken}")
  }

  final protected def requireParser(parser: JsonParser, condition: Boolean, msg: => String): Unit = {
    if (!condition) throw new JsonParseException(parser, msg)
  }
}
