package clean.tex

import java.io.File

import clean.lib._
import ml.Pattern
import ml.classifiers._
import util.{Datasets, Stat, Tempo}

object metaEscolheAlgPCadaStratSoPCT extends AppWithUsage with LearnerTrait with StratsTrait with RangeGenerator with Rank with MetaTrait {
  lazy val arguments = superArguments ++
    List("learners:nb,5nn,c45,vfdt,ci,...|eci|i|ei|in|svm", "rank", "ntrees", "vencedorOuPerdedor(use1):1|-1", "runs", "folds", "ini", "fim", "porPool:p", "guardaSohRank:true|false")

  val context = this.getClass.getName.split('.').last.dropRight(1)
  val dedup = false
  val measure = ALCKappa
  val dsminSize = 100
  //n=2 estraga stats
  val n = 1
  run()

  override def run() = {
    super.run()
    if (guardaSohRank) println(s"Guardanado só rank!")
    if (porPool && (rus != 1 || ks != 90 || !porRank)) justQuit("porPool ativado com parametros errados!")
    val ls = learners(learnersStr)
    val metaclassifs = (patts: Vector[Pattern]) => if (porRank) Vector()
    else Vector(//NB não funciona porque quebra na discretização
      PCT()
    )
    println(s"${metaclassifs} <- metaclassifs")
    val leastxt = learnerStr
    stratsTexForGraficoComplexo foreach { strat =>
      Tempo.start
      val stratName = strat(NoLearner()).limp
      val pares = for {l <- ls} yield strat -> l
      val arq = s"/home/davi/wcs/arff/$context-$porPool-n${if (porRank) 1 else n}best${criterio}m$measure-$ini.$fim-${stratName + (if (porRank) "Rank" else "")}-${leastxt.replace(" ", ".")}-U$dsminSize.arff"
      val labels = pares.map { case (s, l) => s(l).limpa }
      val labelsleas = ls.map {
        _.limpa
      }

      //cada dataset produz um bag de metaexemplos (|bag| >= 25)
      println(s"${datasets.size} <- dssss.size")
      def bagsNaN = datasets.par map { d =>
        val ds = Ds(d, readOnly = true)
        ds.open()
        val (ti0, th0, tf0, tpass) = ranges(ds)
        val ti = ini match {
          case "ti" => ti0
          case "th" => th0 + 1
        }
        val tf = fim match {
          case "th" => th0
          case "tf" => tf0
        }
        val res = for {
          r <- 0 until runs
          f <- 0 until folds
        } yield {
            //descobre vencedores deste pool
            val accs = pares map { case (s, l) =>
              //            p -> measure(ds, Passive(Seq()), ds.bestPassiveLearner, r, f)(ti,tf).read(ds).getOrElse(error("sem medida"))
              (s(NoLearner()).limpa, l.limpa) -> measure(ds, s(l), l, r, f)(ti, tf).read(ds).getOrElse(justQuit("sem medida"))
            }
            //gera metaexemplos
            //            "\"#classes\",\"#atributos\",\"#exemplos\"," +
            //              "\"#exemplos/#atributos\",\"%nominais\",\"log(#exs)\",\"log(#exs/#atrs)\"," +
            //              "skewnessesmin,skewavg,skewnessesmax,skewnessesminByskewnessesmax," +
            //              "kurtosesmin,kurtavg,kurtosesmax,kurtosesminBykurtosesmax," +
            //              "nominalValuesCountmin,nominalValuesCountAvg,nominalValuesCountmax,nominalValuesCountminBynominalValuesCountmax," +
            //              "mediasmin,mediasavg,mediasmax,mediasminBymediasmax," +
            //              "desviosmin,desviosavg,desviosmax,desviosminBydesviosmax," +
            //              "entropiasmin,entropiasavg,entropiasmax,entropiasminByentropiasmax," +
            //              "correlsmin,correlsavg,correlsmax,correlsminBycorrelsmax,correleucmah,correleucman,correlmanmah","AH-conect.-Y", "AH-Dunn-Y", "AH-silhueta-Y", "AH-conect.-1.5Y", "AH-Dunn-1.5Y", "AH-silhueta-1.5Y",
            //            "AH-conect.-2Y", "AH-Dunn-2Y", "AH-silhueta-2Y", "kM-conect.-Y", "kM-Dunn-Y", "kM-silhueta-Y", "kM-conect.-1.5Y", "kM-Dunn-1.5Y",
            //            "kM-silhueta-1.5Y", "kM-conect.-2Y", "kM-Dunn-2Y", "kM-silhueta-2Y"
            //FS não ajudou, mesmo roubando assim:
            val metaatts0 = ds.metaAttsrf(r, f, suav = false).map(x => (x._1, x._2.toString, x._3)) ++ ds.metaAttsFromR(r, f).map(x => (x._1, x._2.toString, x._3))
            val metaatts = ("\"bag_" + pares.size + "\"", ds.dataset, "string") +: metaatts0
            val insts = if (porRank) {
              //rank legivel por clus e ELM
              List(metaatts ++ ranqueia(accs.map(_._2)).zipWithIndex.map { case (x, i) => (s"class$i", x.toString, "numeric") } -> "")
            } else {
              //Acc
              val melhores = pegaMelhores(accs, n)(_._2 * criterio).map(_._1) //não preciso me preocupar com "só pode ter 25 em cada bag de teste", pois não é ranking
              melhores map (m => metaatts -> (m._1 + "-" + m._2))
            }
            //insts.map(x => x._1.tail.map(x => (x._2.toDouble * 100).round / 100d).mkString(" ")) foreach println
            //            println(s"$r $f")
            insts
          }
        ds.close()
        res.flatten
      }
      def bagsCbases = bagsNaN
      if (!new File(arq).exists()) grava(arq, arff(labels.mkString(","), bagsCbases.toList.flatten, print = true, context, porRank))

      val patterns = Datasets.arff(arq, dedup, rmuseless = false) match {
        case Right(x) =>
          //não consegui descobrir como aumentar qtd de atributos no weka (vai ficar sem atts desvio.
          val osbags = x.groupBy(_.base).map(_._2)
          val ps = (osbags map meanPattern(porRank)).toVector
          patts2file(ps, arq + ".arff")
          if (porPool) osbags.flatten.toVector else ps
        case Left(m) => error(s"${m} <- m")
      }

      if (porRank) print(s"$stratName Spearman correl. $rus*$ks-fold CV. " + arq + " ")
      else print(s"$stratName Accuracy. $rus*$ks-fold CV. " + arq + " ")

      val ra = if (porRank) "ra" else "ac"
      val metads = new Db("metanew", readOnly = false)
      metads.open()
      //      select ra,cr,i,f,st,ls,rs,fs,mc,nt,dsminsize from r
      val sql69 = s"select mc from r where porPool='$porPool' and ra='$ra' and cr=$criterio and i='$ini' and f='$fim' and st='$stratName' and ls='$leastxt' and rs=$rus and fs=$ks and nt=$ntrees and dsminsize='$dsminSize'"
      println(s"${sql69} <- sql69")
      metads.readString(sql69) match {
        //        case x: List[Vector[String]] if x.map(_.head).intersect(metaclassifs(Vector()).map(_.limp)).size == 0 =>
        case x: List[Vector[String]] if x.isEmpty | guardaSohRank | true=>
          val cvs = cv(porPool, ini, fim, labelsleas, stratName, ntrees, patterns, metaclassifs, porRank, rus, ks, guardaSohRank).toVector
          val cvsf = cvs.flatten
        case x: List[Vector[String]] => println(s"${x} <- rows already stored")
      }
      metads.close()
      println()
      //      if (!porRank) Datasets.arff(arq, dedup, rmuseless = false) match {
      //        case Right(x) => if (apenasUmPorBase) {
      //          val ps = (x.groupBy(_.base).map(_._2) map meanPattern(porRank)).toVector
      //          patts2file(ps, arq + "umPorBase")
      //          C45(laplace = false, 5, 1).tree(arq + "umPorBase.arff", arq + "umPorBase" + ".tex")
      //          println(s"${arq} <- arq")
      //        }
      //      }
    }
  }
}

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
