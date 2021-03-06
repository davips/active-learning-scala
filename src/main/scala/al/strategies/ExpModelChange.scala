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

package al.strategies

import ml.Pattern
import ml.classifiers.Learner
import ml.models.Model

import scala.util.Random

case class ExpModelChange(learner: Learner, pool: Seq[Pattern], debug: Boolean = false)
   extends StrategyWithLearner {
   override val toString = "Expected Model Change"
   val abr = "EMC"
   val id = 10066
   val s = 100

   protected def next(current_model: Model, unlabeled: Seq[Pattern], labeled: Seq[Pattern]) = {
      val unlabeledSize = unlabeled.size
      val rnd = new Random(unlabeledSize)
      val unlabeledSamp = if (unlabeledSize > s) rnd.shuffle(unlabeled).take(s) else unlabeled
      unlabeledSamp maxBy learner.expected_change(current_model)
   }
}
