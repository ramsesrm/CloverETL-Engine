/* Generated By:JJTree: Do not edit this line. CLVFDoStatement.java */

package org.jetel.interpreter.ASTnode;

import org.jetel.interpreter.ExpParser;
import org.jetel.interpreter.TransformLangParserVisitor;

public class CLVFEvalNode extends SimpleNode {
  public CLVFEvalNode(int id) {
    super(id);
  }

  public CLVFEvalNode(ExpParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
