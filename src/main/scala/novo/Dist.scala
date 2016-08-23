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

package novo

import java.io.FileWriter

import ml.Pattern
import ml.neural.old.Neural
import no.uib.cipr.matrix.{DenseMatrix, DenseVector, MatrixSingularException}
import org.math.array.StatisticSample
import util.Datasets
import weka.core._

case class Dist(pool: Seq[Pattern]) {
  val distance_name = "eucl"
  val dataset = Datasets.patterns2instances(pool)
  lazy val instances_matrix = {
    val it = dataset.iterator()
    var list = List[Array[Double]]()
    while (it.hasNext) list = it.next.toDoubleArray.dropRight(1) :: list
    list.toArray
  }
  lazy val CovMatrixInv = Neural.pinv(new DenseMatrix(StatisticSample.covariance(instances_matrix)))
  lazy val rougherCovMatrixInv = Neural.rougherPinv(new DenseMatrix(StatisticSample.covariance(instances_matrix)))
  lazy val euclidean_ruler = new EuclideanDistance(dataset)
  lazy val minkowski_ruler = new MinkowskiDistance(dataset)
  lazy val manhattan_ruler = new ManhattanDistance(dataset)
  lazy val chebyshev = new ChebyshevDistance(dataset)
  val d = distance_to(distance_name)

  def mahadist(pa: Pattern, pb: Pattern) = {
    //todo: testar
    val x = pa.vector
    val y = pb.vector
    val diff = new DenseMatrix(1, pa.nattributes)
    val difft = new DenseVector(pa.nattributes)
    var i = 0
    val xl = pa.nattributes
    while (i < xl) {
      val v = x(i) - y(i)
      diff.set(0, i, v)
      difft.set(i, v)
      i += 1
    }
    val result = new DenseMatrix(1, pa.nattributes)
    try {
      diff.mult(CovMatrixInv, result)
      val result2 = new DenseVector(1)
      result.mult(difft, result2)
      Math.sqrt(result2.get(0))
    } catch {
      case _: MatrixSingularException => println("Trying with a pinv less prone to singular exceptions...")
        try {
          diff.mult(rougherCovMatrixInv, result)
          val result2 = new DenseVector(1)
          result.mult(difft, result2)
          Math.sqrt(result2.get(0))
        } catch {
          case _: MatrixSingularException => sys.error(s"Singular matrix on mahalanobis calculation  in ${pool.head.dataset().relationName()}!")
        }
    }
  }

  def distance_to(distance_name: String) =
    distance_name match {
      case "eucl" => (pa: Pattern, pb: Pattern) => euclidean_ruler.distance(pa, pb)
      case "manh" => (pa: Pattern, pb: Pattern) => manhattan_ruler.distance(pa, pb)
      case "cheb" => (pa: Pattern, pb: Pattern) => chebyshev.distance(pa, pb)
      case "maha" => (pa: Pattern, pb: Pattern) => mahadist(pa, pb)
      case "mink" => (pa: Pattern, pb: Pattern) => minkowski_ruler.distance(pa, pb)
    }
}

trait DistT{
  def addAtt(dataset: String, patts: Seq[Pattern], allpatts: Seq[Pattern]) = {
    val seqden = Seq(1, 4, 16, 32, 64, 128)
    val di = Dist(allpatts)
    val header = patts.head.dataset.toString.split("\n").takeWhile(!_.contains("@data"))
    val atts = seqden map (i => s"@attribute d$i numeric")
    val newHeader = header.dropRight(2) ++ atts ++ header.takeRight(2)

    def denses(p: Pattern) = seqden map den(di, allpatts, p)
    val newData = patts map { p => (p.toString.split(",").dropRight(1) ++ denses(p) ++ p.toString.split(",").takeRight(1)).mkString(",") }
    val arq = s"/run/shm/$dataset.arff"

    val fw = new FileWriter(arq)
    fw.write((newHeader ++ Seq("@data") ++ newData).mkString("\n"))
    fw.close()
    Datasets.arff(arq, dedup = false, rmuseless = false) match {
      case Right(x) => x
      case Left(e) => sys.error(e)
    }
  }

  def den(di: Dist, pool: Seq[Pattern], x: Pattern)(numNeigs: Int) = {
    val simis = pool map { s => s -> 1d / (1 + di.d(x, s)) }
    val neigs = simis.sortBy(-_._2).take(numNeigs + 1).tail.map(_._2)
    neigs.sum / numNeigs
  }
}
