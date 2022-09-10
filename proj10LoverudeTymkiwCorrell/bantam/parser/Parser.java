/*
 * Authors: Haoyu Song and Dale Skrien
 * Latest change: Oct. 5, 2021
 *
 * In the grammar below, the variables are enclosed in angle brackets and
 * "::=" is used instead of "-->" to separate a variable from its rules.
 * The special character "|" is used to separate the rules for each variable
 * (but note that "||" is an operator).
 * EMPTY indicates a rule with an empty right hand side.
 * All other symbols in the rules are terminals.
 *
 * Modified by Dylan Tymkiw,
 *             Jasper Loverude,
 *             Cassidy Correll
 */
package proj10LoverudeTymkiwCorrell.bantam.parser;

import proj10LoverudeTymkiwCorrell.bantam.lexer.*;
import proj10LoverudeTymkiwCorrell.bantam.util.*;
import proj10LoverudeTymkiwCorrell.bantam.ast.*;
import proj10LoverudeTymkiwCorrell.bantam.util.Error;

import java.util.List;
import java.util.Set;

import static proj10LoverudeTymkiwCorrell.bantam.lexer.Token.Kind.*;

public class Parser
{
    // instance variables
    private Scanner scanner; // provides the tokens
    private Token currentToken; // the lookahead token
    private final ErrorHandler errorHandler; // collects & organizes the error messages
    private String filename;

    //Set of operator Token types
    private final Set<Token.Kind> operatorSet = Set.of(
            BINARYLOGIC, PLUSMINUS, MULDIV, COMPARE, UNARYINCR, UNARYDECR, ASSIGN,
            UNARYNOT);

    private final static Set<String>  reservedWords = Set.of("break", "cast", "class", "var",
            "else", "extends", "for", "if", "instanceof", "new", "return", "while");

    // constructor
    public Parser(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * parse the given file and return the root node of the AST
     * @param filename The name of the Bantam Java file to be parsed
     * @return The Program node forming the root of the AST generated by the parser
     */
    public Program parse(String filename) {

        scanner = new Scanner(filename, errorHandler);
        this.filename = filename;
        currentToken = scanner.scan();

        //Program program = parseProgram();

        return parseProgram();
    }


    // <Program> ::= <Class> | <Class> <Program>
    private Program parseProgram() {
        int position = currentToken.position;
        ClassList clist = new ClassList(position);


        while (currentToken.kind != EOF) {
            Class_ aClass = parseClass();
            clist.addElement(aClass);
        }

        return new Program(position, clist);
    }


    // <Class> ::= CLASS <Identifier> <ExtendsClause> { <MemberList> }
    // <ExtendsClause> ::= EXTENDS <Identifier> | EMPTY
    // <MemberList> ::= EMPTY | <Member> <MemberList>
    private Class_ parseClass() {


        int position = currentToken.position;

        // Check that token is Class keyword, and then parse the identifier
        if (currentToken.kind != CLASS){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    position, "keyword 'Class' expected");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        String className = parseIdentifier();

        String parentName = "Object";

        // If next token is <ExtendsClause>, parse and store parent name
        if(currentToken.kind == EXTENDS){

            currentToken = scanner.scan();

            parentName = parseIdentifier();

        }

        // Check for '{'
        if(currentToken.kind != LCURLY){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "'{' expected");
            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        /* Creates MemberList, populates it with members
           could have 0 or more members  */
        MemberList memberList = new MemberList(currentToken.position);

        while(currentToken.kind != RCURLY){
            // Will either parse a member or will throw an error
            memberList.addElement(parseMember());
        }

        currentToken = scanner.scan();
        // Does not directly check for '}' because parseMember will throw the error
        return new Class_(position, filename, className, parentName, memberList);
    }


    //Fields and Methods
    // <Member> ::= <Field> | <Method>
    // <Method> ::= <Type> <Identifier> ( <Parameters> ) <BlockStmt>
    // <Field> ::= <Type> <Identifier> <InitialValue> ;
    // <InitialValue> ::= EMPTY | = <Expression>
    /**
     * Parse the given member and return member node
     *
     * @return Member node
     */
    private Member parseMember() {

        int position = currentToken.position;

        // Parses <Type> and <Identifier>
        String typeName = parseType();
        String memberName = parseIdentifier();

        // If there is a member, parse between the parentheses
        if(currentToken.kind != LPAREN){

            Expr init = null;

            // Is either empty, or is an expression
            if(currentToken.kind != SEMICOLON){

                currentToken = scanner.scan();

                init = parseExpression();

                // Checks for semicolon
                if(currentToken.kind != SEMICOLON){


                    errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                            currentToken.position, "';' expected");

                    throw new CompilationException(errorHandler);

                }

            }
            currentToken = scanner.scan();

            return new Field(position, typeName, memberName, init);
        }

        // If it is a method, parse parameters
        //currentToken = scanner.scan();

        FormalList formalList = parseParameters();

        BlockStmt stmt = (BlockStmt) parseBlock();
        return new Method(position, typeName, memberName, formalList, stmt.getStmtList());
    }


    //-----------------------------------
    //Statements
    // <Stmt> ::= <WhileStmt> | <ReturnStmt> | <BreakStmt> | <VarDeclaration>
    //             | <ExpressionStmt> | <ForStmt> | <BlockStmt> | <IfStmt>
    /**
     * Parses a statement
     *
     * @return A statement node
     */
    private Stmt parseStatement() {

            Stmt stmt;

            switch (currentToken.kind) {
                case IF:
                    stmt = parseIf();
                    break;
                case LCURLY:
                    stmt = parseBlock();
                    break;
                case VAR:
                    stmt = parseVarDeclaration();
                    break;
                case RETURN:
                    stmt = parseReturn();
                    break;
                case FOR:
                    stmt = parseFor();
                    break;
                case WHILE:
                    stmt = parseWhile();
                    break;
                case BREAK:
                    stmt = parseBreak();
                    break;
                default:
                    stmt = parseExpressionStmt();
            }

            return stmt;
    }


    // <WhileStmt> ::= WHILE ( <Expression> ) <Stmt>
    /**
     * Parse a while statement, and returns its node
     *
     * @return A WhileStmt node
     */
    private Stmt parseWhile() {

        int position = currentToken.position;

        currentToken = scanner.scan();

        // Checks for parenthesis after while,
        if (currentToken.kind != LPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, "Left parenthesis missing");
            throw new CompilationException(errorHandler);
        }

        //
        currentToken = scanner.scan();

        Expr expr = parseExpression();

        // Checks for right parenthesis
        if(currentToken.kind != RPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename , position,
                    "Right parenthesis missing");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        Stmt stmt = parseStatement();

        return new WhileStmt(position, expr, stmt);
    }

    // <ReturnStmt> ::= RETURN <Expression> ; | RETURN ;
    /**
     * Parse the given return statement and return its node
     *
     * @return ReturnStmt node
     */
    private Stmt parseReturn() {
        int position = currentToken.position;
        currentToken = scanner.scan();

        Expr expr;

        if(currentToken.kind == SEMICOLON){
            expr = null;
        }else {
            expr = parseExpression();
        }

        if (currentToken.kind != Token.Kind.SEMICOLON){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    position, "';' expected");
            throw new CompilationException(errorHandler);
        }
        currentToken = scanner.scan();
        return new ReturnStmt(position, expr);

    }


    // <BreakStmt> ::= BREAK ;
    /**
     * Parse the given BreakStmt and return node
     *
     * @return BreakStmt node
     */
    private Stmt parseBreak() {
        int position = currentToken.position;

        currentToken = scanner.scan();

        // Checks for semicolon
        if(currentToken.kind != SEMICOLON){

            errorHandler.register(Error.Kind.PARSE_ERROR, "Semicolon expected");

            throw new CompilationException(errorHandler);

        }

        currentToken = scanner.scan();

        return new BreakStmt(position);
    }


    // <ExpressionStmt> ::= <Expression> ;
    /**
     * Parse the given ExpressionStmt and return its node
     *
     * @return ExpressionStmt node
     */
    private ExprStmt parseExpressionStmt() {

        int position = currentToken.position;
        Expr expr = parseExpression();
        // Checks for semicolon
        if (currentToken.kind != SEMICOLON){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename ,position,"Semicolon expected");
            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        return new ExprStmt(position, expr);
    }

    // <VarDeclaration> ::= VAR <Id> = <Expression> ;
    /**
     * Parse the given VarDeclaration and return DeclStmt node
     *
     * @return DeclStmt node
     */
    private Stmt parseVarDeclaration() {
        currentToken = scanner.scan();
        int position = currentToken.position;

        // Checks that var has identifier
        if (currentToken.kind != Token.Kind.IDENTIFIER){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "!!PLACEHOLDER MESSAGE");

            throw new CompilationException(errorHandler);
        }

        String varName = currentToken.getSpelling();

        currentToken = scanner.scan();

        // Checks that DeclStmt has assign
        if (currentToken.kind != Token.Kind.ASSIGN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "= sign expected");

            throw new CompilationException(errorHandler);
        }

        // Parses the expression and increments token
        currentToken = scanner.scan();
        Expr expr = parseExpression();


        if(currentToken.kind != SEMICOLON){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    position, "; expected" );
            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        return new DeclStmt(position, varName, expr);
    }


    // <ForStmt> ::= FOR ( <Start> ; <Terminate> ; <Increment> ) <STMT>
    // <Start> ::=     EMPTY | <Expression>
    // <Terminate> ::= EMPTY | <Expression>
    // <Increment> ::= EMPTY | <Expression>
    /**
     * Parse the given for loop, and return ForStmt node
     *
     * @return ForStmt node
     */
    private Stmt parseFor() {

        int position = currentToken.position;

        currentToken = scanner.scan();

        // Checks that there is a left parenthesis
        if (currentToken.kind != LPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "'(' Expected");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        Expr init = null;

        // Checks for semicolon
        if(currentToken.kind != SEMICOLON){


            init = parseExpression();
            currentToken = scanner.scan();

        }
        else{

            currentToken = scanner.scan();

        }

        // Parses predicate expression
        //currentToken = scanner.scan();

        Expr predExpr = null;
        if(currentToken.kind != SEMICOLON){
            predExpr = parseExpression();
            currentToken = scanner.scan();


        }
        else{

            currentToken = scanner.scan();

        }

        //currentToken = scanner.scan();

        // Parses the increment statement
        Expr updateExpr =  null;

        // Checks for right parenthesis
        if(currentToken.kind != RPAREN){

            updateExpr = parseExpression();

        }

        if(currentToken.kind != RPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "')' expected");

            throw new CompilationException(errorHandler);
        }

        // Parses for loop body
        currentToken = scanner.scan();

        Stmt bodyStmt = parseStatement();


        return new ForStmt(position, init, predExpr, updateExpr, bodyStmt);
    }


    // <BlockStmt> ::= { <Body> }
    // <Body> ::= EMPTY | <Stmt> <Body>
    /**
     * Parse given BlockStmt and returns its node
     *
     * @return BlockStmt node
     */
    private Stmt parseBlock() {

        // Prepares list of statements
        int position = currentToken.position;

        StmtList stmtList = new StmtList(position);

        // Checks for left curly brace
        if(currentToken.kind != LCURLY){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename, position,
                    "Missing brackets");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        // Checks for right curly brace
        while (currentToken.kind != RCURLY){
            stmtList.addElement(parseStatement());
            if(currentToken.kind == EOF){

                errorHandler.register(Error.Kind.PARSE_ERROR, this.filename, position,
                        "Reached end of file before right curly brace.");

                throw new CompilationException(errorHandler);

            }

        }

        currentToken = scanner.scan();
        return new BlockStmt(position, stmtList);

    }


    // <IfStmt> ::= IF ( <Expr> ) <Stmt> | IF ( <Expr> ) <Stmt> ELSE <Stmt>
    /**
     * Parse the given IfStmt and returns its node
     *
     * @return IfStmt node
     */
    private Stmt parseIf() {

        int position = currentToken.position;

        currentToken = scanner.scan();

        Stmt thenStmt;

        Stmt elseStmt = null;

        // Checks for left parenthesis
        if (currentToken.kind != LPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "Opening parenthesis needed for if statement.");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        Expr expr = parseExpression();

        // Checks for right parenthesis
        if (currentToken.kind != RPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "!!PLACEHOLDER MESSAGE");

            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();

        thenStmt = parseStatement();

        // Checks for else statement
        if (currentToken.kind == ELSE){

            currentToken = scanner.scan();

            elseStmt = parseStatement();
        }

        return new IfStmt(position,expr,thenStmt,elseStmt);
    }


    /*-----------------------------------------
     Expressions
     Here we use different rules than the grammar on page 49
     of the manual to handle the precedence of operations
    */

    // <Expression> ::= <LogicalORExpr> <OptionalAssignment>
    // <OptionalAssignment> ::= EMPTY | = <Expression>
    /**
     * Parse the given member and return member node
     *
     * @return Member node
     */
    private Expr parseExpression() {
        int position = currentToken.position;

        String refName = null;
        VarExpr ref = null;

        String expressionName = currentToken.getSpelling();
        Expr logExpr = parseOrExpr();

        if(logExpr instanceof VarExpr){
            VarExpr varExpr = (VarExpr) logExpr;
            expressionName = varExpr.getName();
            ref = (VarExpr) varExpr.getRef();
        }
        Expr expr;

        // Checks for assignment '='
        if (currentToken.kind == ASSIGN){

            currentToken = scanner.scan();
            expr = parseExpression();

            if (ref != null){
                refName = ref.getName();
            }
            return new AssignExpr(position, refName, expressionName, expr );
        }

        return logExpr;
    }


    // <LogicalOR> ::= <logicalAND> <LogicalORRest>
    // <LogicalORRest> ::= EMPTY |  || <LogicalAND> <LogicalORRest>
    /**
     * Parse the given expression and return its node
     *
     * @return Expr node
     */
    private Expr parseOrExpr() {
        int position = currentToken.position;

        Expr leftExpr;
        leftExpr = parseAndExpr();

        // Checks for or statements
        while (currentToken.getSpelling().equals("||")) {

            currentToken = scanner.scan();
            Expr rightExpr = parseAndExpr();
            leftExpr = new BinaryLogicOrExpr(position, leftExpr, rightExpr);
        }
        return leftExpr;
    }


    // <LogicalAND> ::= <ComparisonExpr> <LogicalANDRest>
    // <LogicalANDRest> ::= EMPTY |  && <ComparisonExpr> <LogicalANDRest>

    /**
     * Parse the given expression and return its node
     *
     * @return Expr node
     */
    private Expr parseAndExpr() {
        int position = currentToken.position;
        Expr leftExpr;

        leftExpr = parseEqualityExpr();

        // Checks for and statements
        while (currentToken.getSpelling().equals("&&")){
            currentToken = scanner.scan();
            Expr rightExpr = parseEqualityExpr();
            leftExpr = new BinaryLogicAndExpr(position, leftExpr, rightExpr);
        }
        return leftExpr;
    }


    // <ComparisonExpr> ::= <RelationalExpr> <equalOrNotEqual> <RelationalExpr> |
    //                      <RelationalExpr>
    // <equalOrNotEqual> ::=  = | !=
    /**
     * Parse the given expression and return its node
     *
     * @return Expr node
     */
    private Expr parseEqualityExpr() {
        int position = currentToken.position;
        Expr leftExpr;

        leftExpr = parseRelationalExpr();

        // While there is an equals or not equals stmt
        if(currentToken.getSpelling().equals("==") || currentToken.getSpelling().equals("!=") ){

            String logOp = currentToken.spelling;
            currentToken = scanner.scan();
            Expr rightExpr = parseRelationalExpr();

            if(logOp.equals("==")){
                leftExpr = new BinaryCompEqExpr(position, leftExpr, rightExpr);
            }
            else{
                leftExpr = new BinaryCompNeExpr(position, leftExpr, rightExpr);
            }

        }
        return leftExpr;

    }


    // <RelationalExpr> ::= <AddExpr> | <AddExpr> <ComparisonOp> <AddExpr>
    // <ComparisonOp> ::= < | > | <= | >= | INSTANCEOF
    /**
     * Parse the given expression and return its node
     *
     * @return Expr node
     */
    private Expr parseRelationalExpr() {
        int position = currentToken.position;
        Expr leftExpr;
        leftExpr = parseAddExpr();

        String name = currentToken.getSpelling();

        // Checks if there is comparison operation
        if(name.equals("<") ||
                name.equals(">") ||
                name.equals("<=") ||
                name.equals(">=") ||
                name.equals("instanceof")){

            if(name.equals("instanceof")){
                currentToken = scanner.scan();
                String type = parseType();
                return new InstanceofExpr(position, leftExpr, type);
            }

            currentToken = scanner.scan();
            Expr rightExpr = parseAddExpr();

            // Creates correct expression for operator
            switch (name){

                case "<":
                    leftExpr = new BinaryCompLtExpr(position, leftExpr, rightExpr);
                    break;
                case ">":
                    leftExpr = new BinaryCompGtExpr(position, leftExpr, rightExpr);
                    break;
                case "<=":
                    leftExpr = new BinaryCompLeqExpr(position, leftExpr, rightExpr);
                    break;
                case ">=":
                    leftExpr = new BinaryCompGeqExpr(position, leftExpr, rightExpr);
                    break;

            }

        }

        return leftExpr;
    }


    // <AddExpr>::＝ <MultExpr> <MoreMultExpr>
    // <MoreMultExpr> ::= EMPTY | + <MultExpr> <MoreMultExpr> | - <MultExpr> <MoreMultExpr>
    /**
     * Parse the given expression and return its node
     *
     * @return MultExpr node
     */
    private Expr parseAddExpr() {
        int position = currentToken.position;
        Expr leftExpr;
        leftExpr = parseMultExpr();

        // Checks for '+' or '-'
        while(currentToken.getSpelling().equals("+") || currentToken.getSpelling().equals("-")){
            String operation = currentToken.getSpelling();
            currentToken = scanner.scan();
            Expr rightExpr = parseMultExpr();

            if(operation.equals("+")){
                leftExpr = new BinaryArithPlusExpr(position, leftExpr, rightExpr);
            }
            else if(operation.equals("-")){
                leftExpr = new BinaryArithMinusExpr(position, leftExpr, rightExpr);
            }
        }
        return leftExpr;

    }


    // <MultiExpr> ::= <NewCastOrUnary> <MoreNCU>
    // <MoreNCU> ::= * <NewCastOrUnary> <MoreNCU> |
    //               / <NewCastOrUnary> <MoreNCU> |
    //               % <NewCastOrUnary> <MoreNCU> |
    //               EMPTY
    /**
     * Parse the given expression and return its node
     *
     * @return MultExpr node
     */
    private Expr parseMultExpr() {
        int position = currentToken.position;
        Expr leftExpr;
        leftExpr = parseNewCastOrUnary();

        // Checks for one of the multiplication/div operators
        while (currentToken.getSpelling().equals("*") ||
                currentToken.getSpelling().equals("/") ||
                currentToken.getSpelling().equals("%")) {
            String operator = currentToken.getSpelling();
            currentToken = scanner.scan();
            Expr rightExpr = parseNewCastOrUnary();

            // Checks for correct operator, creates corresponding node class
            switch (operator) {
                case "*":
                    leftExpr = new BinaryArithTimesExpr(position, leftExpr, rightExpr);
                    //currentToken = scanner.scan();
                    break;
                case "/":
                    leftExpr = new BinaryArithDivideExpr(position, leftExpr, rightExpr);
                    //currentToken = scanner.scan();
                    break;
                default:
                    leftExpr = new BinaryArithModulusExpr(position, leftExpr, rightExpr);
                    //currentToken = scanner.scan();
                    break;
            }

        }
        return leftExpr;
    }

    // <NewCastOrUnary> ::= <NewExpression> | <CastExpression> | <UnaryPrefix>
    /**
     * Parse the given expression and return its node
     *
     * @return Expr node
     */
    private Expr parseNewCastOrUnary() {

        Expr expr;

        // Checks if new, cast, or for default case unary
        if (currentToken.kind == NEW){
            expr = parseNew();
        }
        else if(currentToken.kind == CAST){
            expr = parseCast();
        }
        else{
            expr = parseUnaryPrefix();
        }

        return expr;

    }


    // <NewExpression> ::= NEW <Identifier> ( )
    /**
     * Parse the given expression and return its node
     *
     * @return NewExpr node
     */
    private Expr parseNew() {

        int position = currentToken.position;

        // Checks for 'new' keyword
        if(currentToken.kind != NEW){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "keyword 'new' expected");

            throw new CompilationException(errorHandler);

        }

        currentToken = scanner.scan();
        String type = parseIdentifier();
        // Checks for left parenthesis
        if(currentToken.kind != LPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "'(' expected");

            throw new CompilationException(errorHandler);

        }
        currentToken = scanner.scan();


        // Checks for right parenthesis
        if(currentToken.kind != RPAREN){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "')' expected");
            throw new CompilationException(errorHandler);
        }
        currentToken = scanner.scan();

        return new NewExpr(position, type);
    }


    // <CastExpression> ::= CAST ( <Type> , <Expression> )
    /**
     * Parse the given cast expression and return its node
     *
     * @return CastExpr node
     */
    private Expr parseCast() {
        int position = currentToken.position;

        if(currentToken.kind != CAST){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "keyword 'cast' expected");
            throw new CompilationException(errorHandler);
        }
        currentToken = scanner.scan();
        if(currentToken.kind != LPAREN){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "'(' expected");
            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();
        String type = parseType();

        if(currentToken.kind != COMMA){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "',' expected");

            throw new CompilationException(errorHandler);

        }
        currentToken = scanner.scan();

        Expr expr = parseExpression();

        if(currentToken.kind != RPAREN){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "')' expected");

            throw new CompilationException(errorHandler);

        }

        currentToken = scanner.scan();
        return new CastExpr(position, type, expr);
    }


    // <UnaryPrefix> ::= <PrefixOp> <UnaryPreFix> | <UnaryPostfix>
    // <PrefixOp> ::= - | ! | ++ | --
    /**
     * Parse the given cast unaryPrefix and return its node
     *
     * @return CastExpr node
     */
    private Expr parseUnaryPrefix() {

        int position = currentToken.position;

        Token initialToken = currentToken;


        /*
         InnerExpr is the nested expression inside this UnaryPrefix; the two
         possibilities are either that this is the terminal UnaryPrefix, (eg. ++(Expr),
         or sometimes there are more prefixOp's to parse (eg. ++--(Expr),
         but the innerExpr is everything coming after the prefix
        */
        Expr expr;

        Expr innerExpr;

        //currentToken = scanner.scan();

        if(currentToken.getSpelling().equals("++")  ||
            currentToken.getSpelling().equals("--") ||
            currentToken.getSpelling().equals("!")){

            currentToken = scanner.scan();
            innerExpr = parseUnaryPrefix();

        }else{

            innerExpr = parseUnaryPostfix();
        }

        if(initialToken.getSpelling().equals("++")){

            expr = new UnaryIncrExpr(position, innerExpr,false);

        }else if(initialToken.getSpelling().equals("--")){

            expr = new UnaryDecrExpr(position, innerExpr,false);

        }else if(initialToken.getSpelling().equals("!")){

            expr = new UnaryNotExpr(position, innerExpr);

        }else if(initialToken.getSpelling().equals("-")){

            expr = new UnaryNegExpr(position,  innerExpr);

        }else{
            //currentToken = scanner.scan();
            return innerExpr;

        }

        return expr;

    }


    // <UnaryPostfix> ::= <Primary> <PostfixOp>
    // <PostfixOp> ::= ++ | -- | EMPTY
    private Expr parseUnaryPostfix() {
        int position = currentToken.position;
        Expr primaryExpr = parsePrimary();

        Expr postFixExpr;

        if(currentToken.kind == UNARYINCR) {

            postFixExpr = new UnaryIncrExpr(position, primaryExpr, true);
            currentToken = scanner.scan();
        }
        else if(currentToken.kind == UNARYDECR){

            postFixExpr = new UnaryDecrExpr(position, primaryExpr,true);
            currentToken = scanner.scan();
        }
        else{

            postFixExpr = primaryExpr;

        }

        return postFixExpr;

    }


    // <Primary> ::= ( <Expression> ) | <IntegerConst> | <BooleanConst> |
    //                              <StringConst> | <VarExpr>
    // <VarExpr> ::= <VarExprPrefix> <Identifier> <VarExprSuffix>
    // <VarExprPrefix> ::= SUPER . | THIS . | EMPTY
    // <VarExprSuffix> ::= ( <Arguments> ) | EMPTY
    /**
     * Parse the given expression and return its node
     *
     * @return primaryExpr node
     */
    private Expr parsePrimary() {
        if(currentToken.kind == LPAREN){

            currentToken = scanner.scan();
            Expr expr = parseExpression();

            // Checks for right parenthesis
            if(currentToken.kind != RPAREN){

                errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                        currentToken.position, "')' expected");

                throw new CompilationException(errorHandler);
            }
            return expr;
        }

        //IntConst
        else if(currentToken.kind == INTCONST){
            return parseIntConst();
        }
        //BooleanConst
        else if(currentToken.kind == BOOLEAN){
            return parseBoolean();
        }
        //StringConst
        else if(currentToken.kind == STRCONST){
            return parseStringConst();
        }
        //var expression
        else{
            return parseVarExpr();
        }
    }


    // <VarExpr> ::= <VarExprPrefix> <Identifier> <VarExprSuffix>
    // <VarExprPrefix> ::= SUPER . | THIS . | EMPTY
    // <VarExprSuffix> ::= ( <Arguments> ) | EMPTY
    /**
     * Parse the given var expression and return its node
     *
     * @return VarExpr node
     */
    private Expr parseVarExpr(){
        int position = currentToken.position;

        VarExpr ref;

        VarExpr varExpr;
        if(currentToken.getSpelling().equals("super") || currentToken.getSpelling().equals("this")){

            ref = new VarExpr(position, null, currentToken.spelling);

            currentToken = scanner.scan();


            if(currentToken.kind == DOT){
                currentToken = scanner.scan();
                String name = parseIdentifier();
                varExpr = new VarExpr(position, ref, name);

            }
            else {
                varExpr = ref;
                //currentToken = scanner.scan();
                return varExpr;
            }

        }
        else {
            String name = parseIdentifier();
            varExpr = new VarExpr(position, null, name);

        }


        if(currentToken.kind == LPAREN){
            //DispatchExpr
            currentToken = scanner.scan();
            ExprList arguments = parseArguments();
            return new DispatchExpr(position, varExpr.getRef(), varExpr.getName(),arguments);
        }
        else{
            return varExpr;
        }


    }


    // <Arguments> ::= EMPTY | <Expression> <MoreArgs>
    // <MoreArgs>  ::= EMPTY | , <Expression> <MoreArgs>
    private ExprList parseArguments() {
        int position = currentToken.position;

        ExprList arguments = new ExprList(position);


        if(currentToken.kind == RPAREN){
            currentToken = scanner.scan();
            return arguments;
        }

        while (true){
            arguments.addElement(parseExpression());
            if(currentToken.kind == RPAREN){
                break;
            }
            else if(currentToken.kind == COMMA){
                currentToken = scanner.scan();
            }
            else{
                errorHandler.register(Error.Kind.PARSE_ERROR, this.filename, position,
                        "Invalid list of arguments");
                throw new CompilationException(errorHandler);
            }
        }
        currentToken = scanner.scan();
        return arguments;

    }


    // <Parameters> ::=  EMPTY | <Formal> <MoreFormals>
    // <MoreFormals> ::= EMPTY | , <Formal> <MoreFormals
    private FormalList parseParameters() {
        int position = currentToken.position;

        if(currentToken.kind != LPAREN){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename, position,
                    "( expected");
            throw new CompilationException(errorHandler);
        }

        currentToken = scanner.scan();
        FormalList parameters = new FormalList(position);

        if(currentToken.kind == RPAREN){
            currentToken = scanner.scan();
            return parameters;
        }

        while (true){
            parameters.addElement(parseFormal());
            if(currentToken.kind == RPAREN){
                break;
            }
            else if(currentToken.kind == COMMA){
                currentToken = scanner.scan();
            }
            else{
                errorHandler.register(Error.Kind.PARSE_ERROR, this.filename, position,
                        "Invalid list of parameters");
                throw new CompilationException(errorHandler);
            }
        }
        currentToken = scanner.scan();
        return parameters;
    }


    // <Formal> ::= <Type> <Identifier>
    private Formal parseFormal() {
        int position = currentToken.position;

        String type = parseType();
        String id = parseIdentifier();

        return new Formal(position, type, id);
    }


    // <Type> ::= <Identifier>
    private String parseType() {

        // Checks for type other than var
        if(reservedWords.contains(currentToken.spelling)){

            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "var expected");
            throw new CompilationException(errorHandler);
        }
        String name = currentToken.getSpelling();
        currentToken = scanner.scan();
        return name;
    }



    //----------------------------------------
    //Terminals

    private String parseOperator() {
        if(!operatorSet.contains(currentToken.kind)){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "Operator expected");
            throw new CompilationException(errorHandler);
        }
        String name = currentToken.getSpelling();
        currentToken = scanner.scan();
        return name;
    }


    private String parseIdentifier() {
        if(currentToken.kind != IDENTIFIER){
            errorHandler.register(Error.Kind.PARSE_ERROR, this.filename,
                    currentToken.position, "Identifier expected");
            throw new CompilationException(errorHandler);
        }
        String name = currentToken.getSpelling();
        currentToken = scanner.scan();
        return name;
    }


    private ConstStringExpr parseStringConst() {
        int position = currentToken.position;
        //...save the currentToken's string to a local variable...
        String strConst = currentToken.getSpelling();
        //...advance to the next token...
        currentToken = scanner.scan();
        //...return a new ConstStringExpr containing the string...
        return new ConstStringExpr(position, strConst);
    }


    private ConstIntExpr parseIntConst() {
        int position = currentToken.position;

        String intConst = currentToken.getSpelling();

        currentToken = scanner.scan();

        return new ConstIntExpr(position, intConst);
    }


    private ConstBooleanExpr parseBoolean() {
        int position = currentToken.position;

        String boolConst = currentToken.getSpelling();

        currentToken = scanner.scan();

        return new ConstBooleanExpr(position, boolConst);
    }



    /*It should take any number of command line arguments. Those arguments should be
    names of files. The main method should loop through the files, scanning and parsing each
    one, and printing to System.out the result, namely the name of the file and either the
    error messages when the file was scanned and parsed or a message that scanning and parsing
     were successful for that file.
     */
    public static void main(String[] args) {

        ErrorHandler errorHandler = new ErrorHandler();
        Parser syntacticAnalyzer = new Parser(errorHandler);

        /* Add arguments for Parser.main() at Run -> Edit Configurations*/


        //Parse each file
        for(int i = 0; i < args.length; i++){
            Program program = null;
            try {
                program = syntacticAnalyzer.parse(args[i]);
            }
            catch (CompilationException ex){
               System.out.println(ex.getMessage());
            }
            // Prints filename and number of errors
            System.out.println(args[i]);
            System.out.println(errorHandler.getErrorList().size() + " errors");

            //Print errors if found
            if(errorHandler.errorsFound()){
                List<Error> errorList = errorHandler.getErrorList();
                for(int j = 0; j < errorList.size(); j++){
                    System.out.println(errorList.get(j));
                }
            }
            else{

                System.out.println("Parsing was successful!!");

                proj10LoverudeTymkiwCorrell.bantam.treedrawer.Drawer drawer =
                        new proj10LoverudeTymkiwCorrell.bantam.treedrawer.Drawer();

                drawer.draw(args[i], program);

            }
        }

    }

}

