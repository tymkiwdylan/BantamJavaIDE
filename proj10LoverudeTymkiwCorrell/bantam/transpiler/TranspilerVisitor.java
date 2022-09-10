/*
    File: WriterVisitor
    Names: Jasper Loverude, Dylan Tymkiw, Cassidy Correll
    Class: CS 361
    Project 10
    Date: May 2nd
*/

package proj10LoverudeTymkiwCorrell.bantam.transpiler;

import proj10LoverudeTymkiwCorrell.bantam.ast.*;
import proj10LoverudeTymkiwCorrell.bantam.visitor.Visitor;

import java.util.Iterator;

public class TranspilerVisitor extends Visitor {

    // Writer that translates ASTNodes into legal Java
    private StringBuilder programStringBuilder;

    private String currentIndentation;

    public TranspilerVisitor(){

        super();
        programStringBuilder = new StringBuilder();

        currentIndentation = "";

    }

    /**
     * Increases the spaces for the indentation string
     *
     */
    private void increaseIndentationString(){

        StringBuilder indentationStringBuilder = new StringBuilder(currentIndentation);

        indentationStringBuilder.append("    ");

        currentIndentation = indentationStringBuilder.toString();


    }

    /**
     * Increases the spaces for the indentation string
     *
     */
    private void decreaseIndentationString(){

        StringBuilder indentationStringBuilder = new StringBuilder(currentIndentation);

        /* Conditional should always be true when this method is called-
           this is just a safety measure for future use */
        if(indentationStringBuilder.length() >= 4){
            indentationStringBuilder.delete(0, 4);
        }

        currentIndentation = indentationStringBuilder.toString();


    }



    /**
     * Increases the spaces for the indentation string
     *
     *
     * @return String for indentations
     */
    private String getCurrentIndentation(){

        return currentIndentation;

    }

    /**
     * Visit a list node of classes
     *
     * @param node the class list node
     * @return result of the visit
     */
    public Object visit(ClassList node) {
        for (ASTNode aNode : node) {
            aNode.accept(this);
            programStringBuilder.append("\n\n\n");
        }
        return null;
    }


    /**
     * Visit a class node
     *
     * @param node the class node
     * @return result of the visit
     */
    @Override
    public Object visit(Class_ node) {
        programStringBuilder.append("class " + node.getName());

        if(!node.getParent().equals("Object")){
            programStringBuilder.append(" extends " + node.getParent());
        }

        node.getMemberList().accept(this);
        return null;
    }

    /**
     * Visit a MemberList node
     *
     * @param node the MemberList node
     * @return result of the visit
     */
    @Override
    public Object visit(MemberList node) {

        boolean listHasMembers = (node.getSize() > 0);
        programStringBuilder.append("{ ");
        // Conditional ensures that a class with no members has { } on same line
        if(listHasMembers){
            programStringBuilder.append("\n");
        }
        increaseIndentationString();
        for(ASTNode child : node){
            child.accept(this);
        }
        decreaseIndentationString();
        // Conditional ensures that a class with no members has { } on same line
        if(listHasMembers){
            programStringBuilder.append("\n" + getCurrentIndentation());
        }
        programStringBuilder.append("}");

        return null;

    }

    /**
     * Visit a field node
     *
     * @param node the field node
     * @return result of the visit
     */
    @Override
    public Object visit(Field node) {
        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("protected ");
        programStringBuilder.append(node.getType() + " " + node.getName());

        if (node.getInit() != null) {
            programStringBuilder.append(" = ");
            node.getInit().accept(this);
        }
        programStringBuilder.append(";");
        return null;
    }

    /**
     * Visit a method node
     *
     * @param node the method node
     * @return result of the visit
     */
    @Override
    public Object visit(Method node) {

        programStringBuilder.append("\n\n" + getCurrentIndentation());

        //boolean isMainMethod = (node.getName().equals("main"));

        if (node.getName().equals("main")){
            programStringBuilder.append("public static void main(String[] args)");
        }
        else {
            programStringBuilder.append("public " + node.getReturnType() + " " + node.getName());
            programStringBuilder.append("(");
            node.getFormalList().accept(this);
            programStringBuilder.append(")");
        }
        node.getStmtList().accept(this);
        return null;
    }

    /**
     * Visit a list node of statements
     *
     * @param node the statement list node
     * @return result of the visit
     */
    @Override
    public Object visit(StmtList node) {
        programStringBuilder.append("{");
        increaseIndentationString();
        for (Iterator it = node.iterator(); it.hasNext(); )
            ((Stmt) it.next()).accept(this);
        decreaseIndentationString();
        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("}");
        return null;
    }

    @Override
    public Object visit(FormalList node) {
        for (Iterator it = node.iterator(); it.hasNext(); ) {
            Formal formal = (Formal) it.next();
            formal.accept(this);
            if(it.hasNext()){
                programStringBuilder.append(", ");
            }
        }
        return null;
    }

    /**
     * Visit a formal node
     *
     * @param node the formal node
     * @return result of the visit
     */
    @Override
    public Object visit(Formal node) {
        programStringBuilder.append(node.getType() + " " + node.getName());
        return null;
    }


    /**
     * Visit a declaration statement node
     *
     * @param node the declaration statement node
     * @return result of the visit
     */
    @Override
    public Object visit(DeclStmt node) {
        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("var " + node.getName() + " = ");
        node.getInit().accept(this);
        programStringBuilder.append(";");
        return null;
    }


    /**
     * Visit an expression statement node
     *
     * @param node the expression statement node
     * @return result of the visit
     */
    @Override
    public Object visit(ExprStmt node) {
        programStringBuilder.append("\n" + getCurrentIndentation());
        node.getExpr().accept(this);
        programStringBuilder.append(";");
        return null;
    }

    /**
     * Visit an if statement node
     *
     * @param node the if statement node
     * @return result of the visit
     */
    @Override
    public Object visit(IfStmt node) {
         programStringBuilder.append("\n" + getCurrentIndentation());
         programStringBuilder.append("if(");
         node.getPredExpr().accept(this);
         programStringBuilder.append(")");
         node.getThenStmt().accept(this);
         if (node.getElseStmt() != null) {
             programStringBuilder.append("else");
             node.getElseStmt().accept(this);
         }
         return null;
    }

    /**
     * Visit a while statement node
     *
     * @param node the while statement node
     * @return result of the visit
     */
    @Override
    public Object visit(WhileStmt node) {
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("while(");
        node.getPredExpr().accept(this);
        programStringBuilder.append(")");
        node.getBodyStmt().accept(this);
        return null;
    }

    /**
     * Visit a for statement node
     *
     * @param node the for statement node
     * @return result of the visit
     */
    @Override
    public Object visit(ForStmt node) {
        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("for(");
        if (node.getInitExpr() != null) {
            node.getInitExpr().accept(this);
        }
        programStringBuilder.append("; ");
        if (node.getPredExpr() != null) {
            node.getPredExpr().accept(this);
        }
        programStringBuilder.append("; ");
        if (node.getUpdateExpr() != null) {
            node.getUpdateExpr().accept(this);
        }
        programStringBuilder.append(")");
        node.getBodyStmt().accept(this);
        return null;
    }

    /**
     * Visit a break statement node
     *
     * @param node the break statement node
     * @return result of the visit
     */
    @Override
    public Object visit(BreakStmt node) {

        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("break;");

        return null;
    }



    /**
     * Visit a return statement node
     *
     * @param node the return statement node
     * @return result of the visit
     */
    @Override
    public Object visit(ReturnStmt node) {

        programStringBuilder.append("\n" + getCurrentIndentation());
        programStringBuilder.append("return");
        if(node.getExpr() != null){
            programStringBuilder.append(" ");
            node.getExpr().accept(this);
        }
        programStringBuilder.append(";");
        return null;
    }


    /**
     * Visit a dispatch expression node
     *
     * @param node the dispatch expression node
     * @return result of the visit
     */
    @Override
    public Object visit(DispatchExpr node) {
        if(node.getRefExpr() != null){
            node.getRefExpr().accept(this);
            programStringBuilder.append(".");
        }
        if(node.getMethodName().equals("print")){
            programStringBuilder.append("System.out.println");
        }
        else{
            programStringBuilder.append(node.getMethodName());
        }
        programStringBuilder.append("(");
        node.getActualList().accept(this);
        programStringBuilder.append(")");
        return null;
    }

    @Override
    public Object visit(ExprList node) {
        for (Iterator it = node.iterator(); it.hasNext(); ) {
            ((Expr) it.next()).accept(this);
            if(it.hasNext()){
                programStringBuilder.append(", ");
            }
        }
        return null;
    }


    /**
     * Visit a new expression node
     *
     * @param node the new expression node
     * @return result of the visit
     */
    @Override
    //TODO : this method does not add the parameters used in constructor. Potentially the type should be appended
    // in another visit method?
    public Object visit(NewExpr node) {

        programStringBuilder.append("new ");
        programStringBuilder.append(node.getType());
        programStringBuilder.append("()");
        return null;
    }


    /**
     * Visit an instanceof expression node
     *
     * @param node the instanceof expression node
     * @return result of the visit
     */
    @Override
    public Object visit(InstanceofExpr node) {
        programStringBuilder.append("instanceof ");
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a cast expression node
     *
     * @param node the cast expression node
     * @return result of the visit
     */
    @Override
    public Object visit(CastExpr node) {
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append("(" + node.getType() + ")");
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit an assignment expression node
     *
     * @param node the assignment expression node
     * @return result of the visit
     */
    @Override
    public Object visit(AssignExpr node) {
        //transpiler.writeNodeToJava(node);
        if(node.getRefName() != null){
            programStringBuilder.append(node.getRefName() + ".");
        }
        programStringBuilder.append(node.getName() + " = ");
        node.getExpr().accept(this);
        return null;
    }


    /**
     * Visit a binary comparison equals expression node
     *
     * @param node the binary comparison equals expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompEqExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" == ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     *
     * @param node the binary comparison not equals expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompNeExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" != ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison less than expression node
     *
     * @param node the binary comparison less than expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompLtExpr node) {
        node.getLeftExpr().accept(this);
        programStringBuilder.append(" < ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison less than or equal to expression node
     *
     * @param node the binary comparison less than or equal to expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompLeqExpr node) {
        node.getLeftExpr().accept(this);
        programStringBuilder.append(" <= ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison greater than expression node
     *
     * @param node the binary comparison greater than expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompGtExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" > ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison greater than or equal to expression node
     *
     * @param node the binary comparison greater to or equal to expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryCompGeqExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" >= ");
        node.getRightExpr().accept(this);
        return null;
    }


    /**
     * Visit a binary arithmetic plus expression node
     *
     * @param node the binary arithmetic plus expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryArithPlusExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" + ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic minus expression node
     *
     * @param node the binary arithmetic minus expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryArithMinusExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" - ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic times expression node
     *
     * @param node the binary arithmetic times expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryArithTimesExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" * ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic divide expression node
     *
     * @param node the binary arithmetic divide expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryArithDivideExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" / ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic modulus expression node
     *
     * @param node the binary arithmetic modulus expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryArithModulusExpr node) {
        node.getLeftExpr().accept(this);
        //transpiler.writeNodeToJava(node);
        programStringBuilder.append(" % ");
        node.getRightExpr().accept(this);
        return null;
    }


    /**
     * Visit a binary logical AND expression node
     *
     * @param node the binary logical AND expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryLogicAndExpr node) {
        node.getLeftExpr().accept(this);
        programStringBuilder.append(" && ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary logical OR expression node
     *
     * @param node the binary logical OR expression node
     * @return result of the visit
     */
    @Override
    public Object visit(BinaryLogicOrExpr node) {
        node.getLeftExpr().accept(this);
        programStringBuilder.append(" || ");
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary negation expression node
     *
     * @param node the unary negation expression node
     * @return result of the visit
     */
    @Override
    public Object visit(UnaryNegExpr node) {
        programStringBuilder.append("-");
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary NOT expression node
     *
     * @param node the unary NOT expression node
     * @return result of the visit
     */
    @Override
    public Object visit(UnaryNotExpr node) {
        programStringBuilder.append("!");
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary increment expression node
     *
     * @param node the unary increment expression node
     * @return result of the visit
     */
    @Override
    public Object visit(UnaryIncrExpr node) {

        if (!node.isPostfix()){
            programStringBuilder.append("++");
            node.getExpr().accept(this);
        }
        else{
            node.getExpr().accept(this);
            programStringBuilder.append("++");
        }

        return null;
    }

    /**
     * Visit a unary decrement expression node
     *
     * @param node the unary decrement expression node
     * @return result of the visit
     */
    @Override
    public Object visit(UnaryDecrExpr node) {
        if (!node.isPostfix()){
            programStringBuilder.append("--");
            node.getExpr().accept(this);
        }
        else{
            node.getExpr().accept(this);
            programStringBuilder.append("--");
        }
        return null;
    }

    /**
     * Visit a variable expression node
     *
     * @param node the variable expression node
     * @return result of the visit
     */
    @Override
    public Object visit(VarExpr node) {

        if (node.getRef() != null) {
            node.getRef().accept(this);
            programStringBuilder.append(".");
        }

        //else {
            programStringBuilder.append(node.getName());
        //}

        return null;
    }


    /**
     * Visit an int constant expression node
     *
     * @param node the int constant expression node
     * @return result of the visit
     */
    @Override
    public Object visit(ConstIntExpr node) {

        programStringBuilder.append(node.getIntConstant());

        return null;
    }

    /**
     * Visit a boolean constant expression node
     *
     * @param node the boolean constant expression node
     * @return result of the visit
     */
    @Override
    public Object visit(ConstBooleanExpr node) {

        programStringBuilder.append(node.getConstant());

        return null;
    }

    /**
     * Visit a string constant expression node
     *
     * @param node the string constant expression node
     * @return result of the visit
     */
    @Override
    public Object visit(ConstStringExpr node) {

        programStringBuilder.append(node.getConstant());

        return null;

    }

    public StringBuilder getProgramStringBuilder(){
        return programStringBuilder;
    }

    public void clearProgramStringBuilder(){ programStringBuilder = new StringBuilder(""); }
}
