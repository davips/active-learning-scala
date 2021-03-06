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

import clean.lib.{AppWithUsage, FilterTrait, Ds, CM}
import ml.Pattern
import ml.classifiers._
import ml.models.Model
import org.apache.commons.math3.stat.correlation.{PearsonsCorrelation, SpearmansCorrelation}
import util.Tempo

import scala.util.Random

case class HTU(learner: Learner, pool: Seq[Pattern], distance_name: String, alpha: Double = 1, beta: Double = 1, debug: Boolean = false)
   extends StrategyWithLearnerAndMaps with MarginMeasure with EntropyMeasure {
   override val toString = "HTU a" + alpha + " b" + beta + " (" + distance_name + ")"
   val abr = "\\textbf{HTU" + distance_name.take(3) + "}"
   //+ beta
   val id = if (alpha == 1 && beta == 1 || alpha == 0.5 && beta == 0.5) distance_name match {
      case "eucl" => 4003006 + (100000 * (1 - alpha)).toInt
      case "cheb" => 4003008 + (100000 * (1 - alpha)).toInt
      case "maha" => 4003009 + (100000 * (1 - alpha)).toInt
      case "manh" => 4003007 + (100000 * (1 - alpha)).toInt
   } else throw new Error("Parametros inesperados para HTU.")

   protected def next(mapU: => Map[Pattern, Double], mapL: => Map[Pattern, Double], current_model: Model, unlabeled: Seq[Pattern], labeled: Seq[Pattern]) = {
      val ls = labeled.size
      val us = unlabeled.size
      var mixmax = -1d
      var agmax = -1d
      var xmixmax: Pattern = null
      var xagmax: Pattern = null
      val list_gn_mix = unlabeled map { x =>
         val similarityU = mapU(x) / us
         val similarityL = mapL(x) / ls
         val gn = 1 - margin(current_model)(x)
         val u = math.pow(similarityU, beta)
         val l = math.pow(similarityL, alpha)
         val ag = u / l
         val mix = gn * ag
         if (ag > agmax) {
            agmax = ag
            xagmax = x
         }
         if (mix > mixmax) {
            mixmax = mix
            xmixmax = x
         }
         gn -> mix
      }
      val (a, b) = list_gn_mix.unzip
      lazy val co = new PearsonsCorrelation().correlation(a.toArray, b.toArray)
      if (a.size > 1 && co < 0.999) xagmax else xmixmax
   }

   lazy val poolForLearner: Seq[Pattern] = ???
}