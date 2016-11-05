package onion.compiler

import collection.mutable.ArrayBuffer
import collection.JavaConverters._
import java.io.{IOException, Reader}
import java.util.Arrays.ArrayList
import java.util.Collections

import _root_.onion.compiler.toolbox.Messages
import _root_.onion.compiler.exceptions.CompilationException
import _root_.onion.compiler.parser.{JJOnionParser, ParseException}

class Parsing(config: CompilerConfig) extends AnyRef
  with ProcessingUnit[Array[InputSource], Array[AST.CompilationUnit]] {
  type Environment = Null
  def newEnvironment(source: Array[InputSource]): Null = null
  def doProcess(source: Array[InputSource], environment: Null): Array[AST.CompilationUnit] = {
    def parse(reader: Reader, fileName: String): AST.CompilationUnit = {
      new JJOnionParser(reader).unit().copy(sourceFile = fileName)
    }
    val buffer = new ArrayBuffer[AST.CompilationUnit]()
    val problems = new ArrayBuffer[CompileError]()
    for(i <- 0 until source.length) {
      try {
        buffer += parse(source(i).openReader, source(i).name)
      } catch {
        case e: IOException =>
          problems += new CompileError(null, null, Messages("error.parsing.read_error", source(i).name))
        case e: ParseException =>
          val error = e.currentToken.next
          val expected = e.tokenImage(e.expectedTokenSequences(0)(0))
          problems += new CompileError(source(i).name, new Location(error.beginLine, error.beginColumn), Messages("error.parsing.syntax_error", error.image, expected))
      }
    }
    if(problems.length > 0) throw new CompilationException(problems.asJava)
    buffer.toArray
  }

}
