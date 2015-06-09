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

package clean.tex

import java.io.{File}
import ml.Pattern

import clean.lib._
import ml.classifiers._
import util.{Tempo, Datasets, Stat}

object metaParesByPool extends AppWithUsage with LearnerTrait with StratsTrait with RangeGenerator with Rank with MetaTrait {
  lazy val arguments = superArguments ++ List("learners:nb,5nn,c45,vfdt,ci,...|eci|i|ei|in|svm", "rank")
  val context = this.getClass.getName.split('.').last.dropRight(1)
  val dedup = false

  //se mudar medida, precisa verficar mais dois lugares: dsminSize e no código. ALC é mais fácil.
  //  val measure = Kappa
  val measure = ALCKappa

  //1 100 200 (Kappa exige 200)
  val dsminSize = 1

  //melhores 1; ou piores -1 (-1 é mais difícil pra acc e rank)
  val melhor = 1

  val n = 1
  //n=2 estraga stats
  val featureSel = false

  val (ini, fim) = ("ti", "tf")
  val (rus, ks) = 10 -> 10
  val apenasUmPorBase = true
  run()

  override def run() = {
    super.run()
    Tempo.start
    val ls = learners(learnersStr)
    val metaclassifs = (patts: Vector[Pattern]) => if (porRank) Vector()
    else Vector(//NB não funciona porque quebra na discretização
      CIELMBatch(),
      C45(false, 5),
      //      C45(false, 25),
      KNNBatcha(5, "eucl", patts),
      //      KNNBatcha(25, "eucl", patts),
      RF(42, 5),
      //      SVMLibRBF(),
      NinteraELM(),
      Maj())
    //    stratsTex("all").drop(8) foreach { strat => //drop rnd,clu,atus,qbcrf,svms
    if (porRank) println(s"Pearson correl.: higher is better. $rus*$ks-fold CV. $measure$ini-$fim${if (featureSel) "FeatSel" else ""}")
    else println(s"Accuracy: higher is better. $rus*$ks-fold CV. $n best. $measure$ini-$fim${if (featureSel) "FeatSel" else ""}")
    stratsTex("all") foreach { strat =>
      val stratName = strat(NoLearner()).limp
      println(s"resultados $stratName ===========================")
      val pares = for {l <- ls} yield strat -> l
      //    $rus.$ks
      val arq = s"/home/davi/wcs/arff/$context-n${if (porRank) 1 else n}best${melhor}m$measure-$ini.$fim-${stratName + (if (porRank) "Rank" else "")}-${learnerStr.replace(" ", ".")}-p$dsminSize.arff"
      //      val arq = s"/home/davi/wcs/arff/$context-n${if (porRank) 1 else n}best${melhor}m$measure-$ini.$fim-${pares.map { case (s, l) => s(l).id }.mkString("-").hashCode.toLong.abs + (if (porRank) "Rank" else "")}-${learnerStr.replace(" ", ".")}-p$dsminSize.arff"
      val labels = pares.map { case (s, l) => s(l).limpa }

      //cada dataset produz um bag de metaexemplos (|bag| >= 25)
      def bagsNaN = DsByMinSize(datasets, dsminSize).par map { d =>
        val ds = Ds(d, readOnly = true)
        ds.open()
        val (ti0, th0, tf0, tpass) = ranges(ds)
        val ti = if (ini == "ti") ti0 else th0
        val tf = if (fim == "th") th0 else tf0
        val res = for {
          r <- 0 until runs
          f <- 0 until folds
        } yield {
            //descobre vencedores deste pool
            val accs = pares map { case (s, l) =>

              //            p -> measure(ds, Passive(Seq()), ds.bestPassiveLearner, r, f)(ti,tf).read(ds).getOrElse(error("sem medida"))

              (s(NoLearner()).limpa, l.limpa) -> measure(ds, s(l), l, r, f)(ti, tf).read(ds).getOrElse(error("sem medida"))

              //            val classif = BestClassifCV100_10foldReadOnlyKappa(ds, r, f, s(l))
              //            s(l) -> measure(ds, s(l), classif, r, f)(-2).read(ds).getOrElse(error("sem medida"))

            }
            //gera metaexemplos
            val metaatts0 = ds.metaAttsrf(r, f).map(x => (x._1, x._2.toString, x._3)) ++ ds.metaAttsFromR(r, f).map(x => (x._1, x._2.toString, x._3))
            val metaatts = ("\"bag_" + pares.size + "\"", ds.dataset, "string") +: metaatts0
            if (porRank) {
              //rank legivel por clus e ELM
              List(metaatts ++ ranqueia(accs.map(_._2)).zipWithIndex.map { case (x, i) => (s"class$i", x.toString, "numeric") } -> "")
            } else {
              //Acc
              val melhores = pegaMelhores(accs, n)(_._2 * melhor).map(_._1)
              melhores map (m => metaatts -> (m._1 + "-" + m._2))
            }
          }
        ds.close()
        res.flatten
      }
      def bags = bagsNaN
      if (!new File(arq).exists()) grava(arq, arff(labels.mkString(","), bags.toList.flatten, print = true, context, porRank))
      //      println(s"$arq")

      val patterns = Datasets.arff(arq, dedup, rmuseless = false) match {
        case Right(x) => if (apenasUmPorBase) {
          val ps = (x.groupBy(_.base).map(_._2) map meanPattern(porRank)).toVector
          patts2file(ps, "umPorBase" + arq)
          println(s"Apenas um por base = ${ps.size}! Apenas um por base!")
          println("umPorBase" + s"$arq")
          ps
        } else x
        case Left(m) => error(s"${m} <- m")
      }

      val porMetaLea = cv(featureSel, patterns, metaclassifs, porRank, rus, ks).toVector.flatten.flatten.groupBy(_.metalearner)
      def fo(x: Double) = "%2.3f".format(x)

      val out = porMetaLea map { case (nome, resultados) =>
        val accTs = Stat.media_desvioPadrao(resultados.map(_.accTs))
        val accTr = Stat.media_desvioPadrao(resultados.map(_.accTr))
        (nome, accTs) -> s"${nome.padTo(8, " ").mkString}:\t${fo(accTr._1)}/${fo(accTr._2)}\t${fo(accTs._1)}/${fo(accTs._2)}"
      }
      out.toList.sortBy(_._1._2).reverseMap(_._2) foreach println

      println("histogramas ===========================")
      porMetaLea foreach { case (nome, resultados) =>
        val r = resultados reduce (_ ++ _)
        println(s"$nome: ------------")
        println(s"treino")
        r.histTr.padTo(6, "   ").zip(r.histTrPred.padTo(6, "   ")).map(x => x._1 + "\t\t" + x._2).take(6) foreach println
        println(s"teste")
        r.histTs.padTo(6, "   ").zip(r.histTsPred.padTo(6, "   ")).map(x => x._1 + "\t\t" + x._2).take(6) foreach println
        println()
      }

    }
    Tempo.print_stop
  }
}