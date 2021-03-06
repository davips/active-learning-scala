/*
mls: basic machine learning algorithms for Scala
Copyright (C) 2014 Davi Pereira dos Santos

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
package ml.classifiers

import al.strategies.Strategy
import clean.lib.{Db, Ds, LearnerTrait}
import ml.Pattern
import ml.models.{Model, WekaBatModel, WekaBatModel2}

case class MetaLearner(pool: Seq[Pattern], fpool: Seq[Pattern], todos: Map[Int, Pattern], ftodos: Map[Int, Pattern], learnerSeed: Int, ds: Ds, st: Strategy, leas: Seq[String], r: Int = -1, f: Int = -1)(mc: String)
  extends Learner with LearnerTrait {
  override val toString = "ML" + mc + leas
  val abr = s"Meta-$mc"
  val boundaryType = "nenhum"
  val attPref = "nenhum"
  val fs = 90
  lazy val id = {
    val rrr = "metade" match {
      case "metade" => (if (r == -1 && f == -1) 100000000 else 200000000) + ((mc +: leas.toList).hashCode % 100000000).abs //94172909: maior sid+convlid até então.
      //    case "inteira" => 200000000 + ((mc +: leas).hashCode % 100000000).abs
    }
    ds.log(s"${rrr} <- id   $leas $mc", 0)
    rrr
  }
  //id não precisa (nem deve for the sake of querying the database) conter st, pois no BD já vem a strat nas tabelas q e h via tabela p

  lazy val (best1, best2) = {
    if (pool.head.vector.sameElements(ftodos(pool.head.id).vector)) throw new Error("filtragem inócua detectada: afeta uma premissa importante em MEtaLearner")
    val metads = new Db("metanew", readOnly = true)
    metads.open()
    val sqls = Seq(
      s"select pre from e where ds='$ds' and fs=$fs and i='ti' and f='th' and st='${st.limp}' and leas='$leas' and mc='$mc' and run=$r and fold=$f;",
      s"select pre from e where ds='$ds' and fs=$fs and i='th' and f='tf' and st='${st.limp}' and leas='$leas' and mc='$mc' and run=$r and fold=$f;"
    )
    val reses = sqls map { sql =>
      print(s"metasql: $sql\t\t")
      metads.readString(sql) match {
        case List(Vector(pre)) => pre //as vezes tinha strat junto, as vezes nao tinha; acho que agora arrumei na origem do problema
        case x => sys.error(x.toString())
      }
    }
    metads.close()
    println("preditos: " + reses)
    reses(0) -> reses(1)
  }
  lazy val ls = leas map str2learner(pool, learnerSeed)
  lazy val leamap = ls.map { lele =>
    lele.limpa -> lele
  }.toMap

  lazy val bestleacomeco = leamap(best1)
  lazy val bestleafinal = leamap(best2)

  /** 98
    * Best learner for the AMOUNT OF patternS.
    */
  def bestlea(n: Int) = if (n <= 50) bestleacomeco else bestleafinal

  def update(model: Model, fast_mutable: Boolean, semcrescer: Boolean)(pattern: Pattern) = {
    val batmodel = cast2wekabatmodel2(model)
    val labeled = batmodel.labeled
    val bl = bestlea(labeled.size + 1)

    val tr = if (labeled.size == 50) {
      //transição
      if (bl.querFiltro) (pattern +: labeled) map (p => ftodos(p.id))
      else (pattern +: labeled) map (p => todos(p.id))
    } else batmodel.adequaFiltragem(pattern) +: labeled

    //esse build é do learner!
    val m0 = bl.build(tr).asInstanceOf[WekaBatModel]
    WekaBatModel2(m0.batch_classifier, m0.training_set, ftodos)
  }

  /**
   * Deve ser chamado apenas para a construção do
   * primeiro modelo.
   * @param labeled
   * @return
   */
  def build(labeled: Seq[Pattern]) = {
    val bl = if (labeled.size != labeled.head.nclasses) {
      //      println(s"SGmulti detectado no $abr")
      bestlea(labeled.count(_.weight() == 1))
    } else bestlea(labeled.size)
    val tr = if (bl.querFiltro) labeled map (p => ftodos(p.id)) else labeled map (p => todos(p.id))

    //esse build é do learner!
    val m0 = bl.build(tr).asInstanceOf[WekaBatModel]
    WekaBatModel2(m0.batch_classifier, m0.training_set, ftodos)
  }

  protected def cast2wekabatmodel2(model: Model) = model match {
    case m: WekaBatModel2 => m
    case _ => throw new Exception("BatchLearner requires WekaBatModel2.")
  }

  def expected_change(model: Model)(pattern: Pattern) = ???
}
