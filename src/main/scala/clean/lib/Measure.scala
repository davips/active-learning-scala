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

package clean.lib

import al.strategies.{Passive, Strategy}
import ml.classifiers.Learner

class NoPidForNonPassive(str: String) extends Exception(str)

trait Measure extends CM with Blob {
  val id: Int
  val ds: Ds
  val s: Strategy
  val l: Learner
  val r: Int
  val f: Int
  val value: Option[Double]
  val forcePid: Boolean
  val context = "MeaTrait"
  protected val instantFun: (Array[Array[Int]]) => Double
  lazy val existia = {
    val tmp = ds.read(s"select count(0) from r where m=$id and p=$pid").head.head
    if (tmp > 1) ds.error(s"Mais do que um r!!!")
    tmp == 1
  }
  lazy val pid = ds.poolId(s, l, r, f).getOrElse {
    if (ds.readOnly) throw new Exception(s"readOnly: Could not create pid for ${(s, l, r, f)}.")
    ds.log(s"Tentando criar pool ${(s, l, r, f)}", 30)
    if (s.id == 22 || forcePid) {
      ds.write(s"insert into p values (NULL, ${s.id}, ${l.id}, $r, $f)")
      ds.poolId(s, l, r, f).getOrElse(throw new Exception(s"Could not create pid for ${(s, l, r, f)}."))
    } else throw new NoPidForNonPassive(s"No pid for ${(s, l, r, f)}.")
  }

  def readAll(ds: Ds): Option[List[Double]]

  def read(ds: Ds) = ds.read(s"select v from r where m=$id and p=$pid") match {
    case List(Vector(v)) => Some(v)
    case List() => None
    case x => ds.error(s"Mais de um valor? $x.")
  }

  def write(ds: Ds, cm: Array[Array[Int]] = null) {
    if (!existia) {
      if (cm != null) ds.write(s"insert into r values ($id, $pid, ${instantFun(cm)})")
      else value match {
        case Some(v) => ds.write(s"insert into r values ($id, $pid, $v)")
        case None => throw new Exception(s"Pool $r.$f incompleto. Impossivel calcular a medida $this.")
      }
    }
  }

  def sqlToWrite(ds: Ds, cm: Array[Array[Int]] = null) = {
    if (!existia) {
      if (cm != null) s"insert into r values ($id, $pid, ${instantFun(cm)})"
      else value match {
        case Some(v) => s"insert into r values ($id, $pid, $v)"
        case None => throw new Exception(s"Pool $r.$f incompleto. Impossivel calcular a medida $this.")
      }
    }
    else {
      //         ds.log(s"Pool $r.$f já existia para a medida $this.", 20)
      "select 1"
    }
  }
}

sealed trait InstantMeasure extends Measure {
  val t: Int
  val forcePid: Boolean
  protected lazy val cms = {
    if (t < ds.nclasses - 1 || t >= ds.expectedPoolSizes(Global.folds).min)
      ds.error(s"t $t fora dos limites t:[${ds.nclasses};${ds.expectedPoolSizes(Global.folds).min}]")
    ds.getCMs(pid)(t, t)
  }
  lazy val value = if (cms.isEmpty) {
    ds.log("Empty cms!", 40)
    None
  } else Some(instantFun(cms(t)))
}

sealed trait RangeMeasure extends Measure {
  val forcePid = false
  val ti: Int
  val tf: Int
  protected val rangeFun: (Seq[Array[Array[Int]]]) => (Array[Array[Int]] => Double) => Double
  protected lazy val cms = {
    if (ti > tf || tf <= ds.nclasses || tf >= ds.expectedPoolSizes(Global.folds).min)
      ds.error(s"ti $ti ou tf $tf fora dos limites ti<=tf tf:]${ds.nclasses};${ds.expectedPoolSizes(Global.folds).min}[")
    ds.getCMs(pid)(ti, tf)
  }
  protected lazy val calc = rangeFun(cms.values.toSeq)
  lazy val value = if (cms.size != tf - ti + 1) None else Some(calc(instantFun))
}

case class BalancedAcc(ds: Ds, s: Strategy, l: Learner, r: Int, f: Int, forcePid: Boolean = false)(val t: Int)
  extends InstantMeasure {
  override val toString = "acurácia balanceada"
  val id = 100000000 + t * 10000
  protected val instantFun = accBal _

  def readAll(ds: Ds) = ds.read(s"select v from r where m >= ${100000000 + 10000} and m <= ${100000000 + 10000 * 199} and p=$pid order by m") match {
    case List() => None
    case lst: List[Vector[Double]] => Some(lst.map(_.head))
    case x => ds.error(s"Retorno estranho: $x.")
  }
}

case class Kappa(ds: Ds, s: Strategy, l: Learner, r: Int, f: Int, forcePid: Boolean = false)(val t: Int)
  extends InstantMeasure {
  override val toString = "kappa"
  val id = 200000000 + t * 10000
  protected val instantFun = kappa _

  def readAll(ds: Ds) = ds.read(s"select v from r where m >= ${200000000 + 10000} and m <= ${200000000 + 10000 * 199} and p=$pid order by m") match {
    case List() => ???; None
    case lst: List[Vector[Double]] => ???; Some(lst.map(_.head))
    case x => ds.error(s"Retorno estranho: $x.")
  }

  def readAll99(ds: Ds) = ds.read(s"select v from r where m >= ${200000000 + 10000} and m <= ${200000000 + 10000 * 99} and p=$pid order by m") match {
    case List() => None
    case lst: List[Vector[Double]] => Some(lst.map(_.head))
    case x => ds.error(s"Retorno estranho: $x.")
  }
}

case class ALCBalancedAcc(ds: Ds, s: Strategy, l: Learner, r: Int, f: Int)(val ti: Int, val tf: Int)
  extends RangeMeasure {
  val id = 300000000 + ti * 10000 + tf
  protected val instantFun = accBal _
  protected val rangeFun = ALC _

  def readAll(ds: Ds) = ???
}

case class ALCKappa(ds: Ds, s: Strategy, l: Learner, r: Int, f: Int)(val ti: Int, val tf: Int)
  extends RangeMeasure {
  override val toString = "ALC-kappa"
  val id = 400000000 + ti * 10000 + tf
  protected val instantFun = kappa _
  protected val rangeFun = ALC _

  def readAll(ds: Ds) = ???
}
