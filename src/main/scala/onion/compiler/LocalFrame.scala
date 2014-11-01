/* ************************************************************** *
 *                                                                *
 * Copyright (c) 2005-2012, Kota Mizushima, All rights reserved.  *
 *                                                                *
 *                                                                *
 * This software is distributed under the modified BSD License.   *
 * ************************************************************** */
package onion.compiler

import java.util
import scala.collection.mutable

/**
 * @author Kota Mizushima
 *
 */
class LocalFrame(val parent: LocalFrame) {
  var scope = new LocalScope(null)
  var closed: Boolean = false

  private val allScopes: mutable.Buffer[LocalScope] = mutable.Buffer[LocalScope]()
  private var maxIndex: Int = 0

  allScopes += scope

  def open[A](block: => A): A = {
    try {
      scope = new LocalScope(scope)
      allScopes += scope
      block
    } finally {
      scope = scope.parent
    }
  }

  def entries: Array[LocalBinding] = {
    val binds: Array[LocalBinding] = entrySet.toArray
    util.Arrays.sort(binds, new util.Comparator[LocalBinding] {
      def compare(b1: LocalBinding, b2: LocalBinding): Int = {
        val i1 = b1.index
        val i2 = b2.index
        if (i1 < i2) -1 else if (i1 > i2) 1 else 0
      }
    })
    binds
  }

  def add(name: String, `type` : IRT.TypeRef): Int = {
    val bind = scope.get(name)
    bind match {
      case Some(_) => -1
      case None =>
        val index = maxIndex
        maxIndex += 1
        scope.put(name, new LocalBinding(index, `type`))
        index
    }
  }

  def lookup(name: String): ClosureLocalBinding = {
    var frame: LocalFrame = this
    var frameIndex: Int = 0
    while (frame != null) {
      val binding = frame.scope.lookup(name)
      if (binding != null) {
        return new ClosureLocalBinding(frameIndex, binding.index, binding.tp)
      }
      frameIndex += 1
      frame = frame.parent
    }
    null
  }

  def lookupOnlyCurrentScope(name: String): ClosureLocalBinding = {
    for(binding <- scope.get(name)) {
      return new ClosureLocalBinding(0, binding.index, binding.tp)
    }
    null
  }

  def setAllClosed(closed: Boolean): Unit = {
    var frame: LocalFrame = this
    while (frame != null) {
      frame.closed = closed
      frame = frame.parent
    }
  }

  def depth: Int = {
    var frame: LocalFrame = this
    var depth: Int = -1
    while (frame != null) {
      depth += 1
      frame = frame.parent
    }
    depth
  }

  private def entrySet: mutable.Set[LocalBinding] = {
    val entries = new mutable.HashSet[LocalBinding]()
    mutable.Set[LocalBinding]() ++ (for (s1 <- allScopes; s2 <- s1.entries) yield s2)
  }

}
