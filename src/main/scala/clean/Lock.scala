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

package clean

import java.io.{File, FileInputStream}
import java.sql.{Connection, DriverManager}

import org.sqlite.SQLiteConnection

import scala.util.Random

trait Lock {
  private var available = true
  lazy val rnd = new Random(System.currentTimeMillis())

  def acquire() = {
    synchronized {
      while (!available) wait()
      available = false
    }
  }

  def release() = {
    synchronized {
      available = true
      notify()
    }
  }
}

