/*

active-learning-scala: Active Learning library for Scala
Copyright (c) 2014 Davi Pereira dos Santos

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package clean.run

import al.strategies.ExpErrorReductionMargin
import clean.nonQ
import ml.Pattern

object EER extends nonQ {
  val context = "EERapp"
  init()

  def strats(pool: Seq[Pattern], learnerSeed: Int) = List(
    ExpErrorReductionMargin(learner(pool, learnerSeed), pool, "entropy", samplingSize),
    ExpErrorReductionMargin(learner(pool, learnerSeed), pool, "gmeans+residual", samplingSize),
    ExpErrorReductionMargin(learner(pool, learnerSeed), pool, "accuracy", samplingSize)
  )
}
