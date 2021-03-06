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
import ml.classifiers._
import ml.models.Model
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation

case class HTUFixo(poolForLearner: Seq[Pattern], learner: Learner, pool: Seq[Pattern], distance_name: String, alpha: Double = 1, beta: Double = 1, debug: Boolean = false, pearson: Double = 0.999, print: Boolean = false)
  extends StrategyWithLearnerAndMaps with MarginMeasure with EntropyMeasure {
  override lazy val toString = "HTU" + learner.limpa + " a" + alpha + " b" + beta + " (" + distance_name + ")"
  lazy val abr = "\\textbf{HTU" + distance_name.take(3) + "}"

  //+ beta
  lazy val id = if (alpha == 1 && beta == 1 || alpha == 0.5 && beta == 0.5) distance_name match {
    case "eucl" => 94172006 + convlid(learner.id)
    case "cheb" => 94172008 + convlid(learner.id)
    case "maha" => 94172009 + convlid(learner.id)
    case "manh" => 94172007 + convlid(learner.id)
  } else throw new Error("Parametros inesperados para HTURF.")

  protected def next(mapU: => Map[Pattern, Double], mapL: => Map[Pattern, Double], current_model: Model, unlabeled: Seq[Pattern], labeled: Seq[Pattern]) = {
    val ls = labeled.size
    val us = unlabeled.size
    var mixmax = -1d
    var agmax = -1d
    var xTUmax: Pattern = null
    var xATUmax: Pattern = null
    val list_gn_mix = unlabeled map { x =>
      val similarityU = mapU(x) / us
      val similarityL = mapL(x) / ls
      val gn = 1 - margin(current_model)(m(x.id))
      val u = math.pow(similarityU, beta)
      val l = math.pow(similarityL, alpha)
      val ag = u / l
      val mix = gn * ag
      if (ag > agmax) {
        agmax = ag
        xATUmax = x
      }
      if (mix > mixmax) {
        mixmax = mix
        xTUmax = x
      }
      (ag, gn, mix)
    }
    val (_, mar, tu) = list_gn_mix.unzip3
    lazy val co = new PearsonsCorrelation().correlation(mar.toArray, tu.toArray)
    if (print) {
      val atumx = list_gn_mix.map(_._1).max
      val marmx = list_gn_mix.map(_._2).max
      val tumx = list_gn_mix.map(_._3).max
      println(s"$ls $atumx $marmx $tumx $co")
    }

    if (mar.size > 1 && co < pearson) xATUmax else xTUmax
  }
}