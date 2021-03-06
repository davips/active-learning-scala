package clean.lib

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

trait CM extends Log {
   def printConfusion(m: Array[Array[Int]]) {
      m foreach { r =>
         r.foreach(c => print(s"$c "))
         println
      }
      println
   }

   def contaAcertos(m: Array[Array[Int]]) = {
      val n = m.length
      var i = 0
      var s = 0
      while (i < n) {
         s += m(i)(i)
         i += 1
      }
      s
   }

   def acc(m: Array[Array[Int]]) = contaAcertos(m).toDouble / contaTotal(m)

   def contaTotal(m: Array[Array[Int]]) = {
      val n = m.length
      var i = 0
      var j = 0
      var s = 0
      while (i < n) {
         j = 0
         while (j < n) {
            s += m(i)(j)
            j += 1
         }
         i += 1
      }
      s
   }

   def gmeans(cms: Array[Array[Int]]) = math.pow(accPorClasse(cms)._1.product, 1d / cms.head.size)

   def accBal(cms: Array[Array[Int]]) = {
      val ac = accPorClasse(cms)
      ac._1.sum / ac._2
   }

   def totPerLin(cms: Array[Array[Int]]) = cms.map(_.sum)

   def totPerCol(cms: Array[Array[Int]]) = totPerLin(cms.transpose)

   def dot(u1: Array[Double], u2: Array[Double]) = {
      val n = u1.length
      var i = 0
      var res = 0d
      while (i < n) {
         res += u1(i) * u2(i)
         i += 1
      }
      res
   }

   def kappa(cm: Array[Array[Int]]) = {
      val tot = contaTotal(cm).toDouble
      val sumDiag = contaAcertos(cm)
      val v1 = totPerCol(cm)
      val v2 = totPerLin(cm)
      val u1 = v1.map(_ / tot)
      val u2 = v2.map(_ / tot)
      val p = dot(u1, u2)
      val pc = if (p == 1) 0 else p
      ((sumDiag / tot) - pc) / (1 - pc)
   }

   def accPorClasse(m: Array[Array[Int]]) = {
      var classesPresentes = m.head.length
      val r = m.zipWithIndex map { case (li, idx) =>
         val s = li.sum.toDouble
         if (s == 0) {
            classesPresentes -= 1
            0d
         } else li(idx) / s
         //error("accPorClasse: Pelo menos uma classe não aparece no conjunto de teste!")
      }
      r -> classesPresentes
   }

   def ALC(CMs: Seq[Array[Array[Int]]])(f: Array[Array[Int]] => Double) = (CMs map f).sum / CMs.size
}

object testaKappa extends App with CM {
  val context = ""
  println(s"${kappa(Array(Array(0, 9), Array(9, 0)))} <- kappa(Array(Array(   ...)))")
}