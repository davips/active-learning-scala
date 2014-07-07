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
import ml.neural.old.Neural
import no.uib.cipr.matrix.{MatrixSingularException, DenseMatrix, DenseVector}
import org.math.array.StatisticSample

trait StrategyWithMahala extends StrategyWithLearner with DistanceMeasure {
  lazy val maha_at_pool_for_mean = mahalanobis_to_mean(distinct_pool)

  private def premaha(patterns: Seq[Pattern]) = Neural.pinv(new DenseMatrix(StatisticSample.covariance((patterns map (_.array)).toArray)))

  /**
   * Mahalanobis distance of a point to the mean.
   */
  protected def mahalanobis_to_mean(pool_patterns: Seq[Pattern]) = {
    val Sinv = premaha(pool_patterns)
    (patterns_to_average: Seq[Pattern]) => {
      val patterns_matrix = (patterns_to_average map (_.array)).toArray
      val mean = StatisticSample.mean(patterns_matrix)
      (pa: Pattern) => mahalanobis0(mean, Sinv)(pa)
    }
  }

  /**
   * Mahalanobis distance of a point to Y.
   */
  protected def mahalanobis_to(pool_patterns: Seq[Pattern]) = {
    val Sinv = premaha(pool_patterns)
    (y: Pattern) => (pa: Pattern) => mahalanobis0(y.array, Sinv)(pa)
  }

  protected def mahalanobis0(mean: Array[Double], Sinv: DenseMatrix)(pa: Pattern): Double = {
    //todo: testar
    val x = pa.vector
    val diff = new DenseMatrix(1, pa.nattributes)
    val difft = new DenseVector(pa.nattributes)
    var i = 0
    val xl = pa.nattributes
    while (i < xl) {
      val v = x(i) - mean(i)
      diff.set(0, i, v)
      difft.set(i, v)
      i += 1
    }
    val result = new DenseMatrix(1, pa.nattributes)
    try {
      diff.mult(Sinv, result)
      val result2 = new DenseVector(1)
      result.mult(difft, result2)
      Math.sqrt(result2.get(0))
    } catch {
      case MatrixSingularException => println("Singular matrix on mahalanobis calculation! Falling back to euclidean...")
        distance_to("eucl")(pa, Pattern(823476234, mean.toList, 0, 1, missed = false, pa.parent))
    }
  }
}

