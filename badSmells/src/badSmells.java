import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import javax.swing.plaf.nimbus.State;
import java.io.FileInputStream;
import java.lang.reflect.Field;


public class badSmells {

    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("Grid.java");

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        MethodAndClassDeclarationVisitor md = new MethodAndClassDeclarationVisitor();
        MessageChainVisitor mc = new MessageChainVisitor();


        cu.accept(md, null);
        cu.accept(mc, null);
    }

    private static class MessageChainVisitor extends VoidVisitorAdapter {

        public void visit(MethodCallExpr call, Object arg) {
            if (call.getScope().isPresent() && call.getScope().get() instanceof MethodCallExpr) {
                System.out.println("Warning! Message Chain Detected: " + call.getScope().get() + " -> " + call.getName());
            }
            super.visit(call, arg);
        }
    }

    private static class MethodAndClassDeclarationVisitor extends VoidVisitorAdapter {

        public void visit(MethodDeclaration md, Object arg) {
            BlockStmt body = md.getBody().get();

            // Long Parameter List
            if (md.getParameters().size() > 5) {
                System.out.println("Warning: long parameter list in method " + md.getName() + "!\n");
            }

            // Long Method (Easy: counting all lines of code - only including lines from the method body)
            if ((body.getEnd().get().line) - (body.getBegin().get().line) - 1 > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many lines of code!\n");
            }

            // Long Method (Medium: counting statements - only including lines within the method declaration's curly braces)
            // All curly brace sets are counted as statements except the set belonging to the method declaration
            if(md.findAll(Statement.class).size() - 1 > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many statements!\n");
            }

            super.visit(md, arg);
        }
//                           _____________________________________________________________________
//                           | KEEP THE BELOW METHOD, JUST IN CASE (Works the same as md.findAll)|
//                           ---------------------------------------------------------------------
//        public int searchStatements(Node statementContents, int statementCounter) {
//            if(statementContents.getChildNodes().size() > 1) {
//                Node statement = statementContents.getChildNodes().get(1);
//                for (Node statementLine : statement.getChildNodes()) {
//                    System.out.println(statementLine + " statement line");
//                    statementCounter++;
//                    if ((statementLine.findAncestor(IfStmt.class).isPresent() || statementLine.findAncestor(WhileStmt.class).isPresent())
//                            && statementLine.getChildNodes().size() > 1) {
//                        searchStatements(statement, statementCounter);
//                    }
//                }
//            }
//            return statementCounter;
//        }

        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {

            // Large Class (Easy: counting all lines of code - only including lines from the method body)
            if((cd.getEnd().get().line) - (cd.getBegin().get().line) - 1 > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many lines\n");
            }

            // Large Class (Medium: Counting all statements - only inside the class body (not including the set of curly braces belonging to the class declaration))
            int statementCounter = cd.findAll(Statement.class).size();
            statementCounter += ((cd.findAll(ClassOrInterfaceDeclaration.class).size() - 1) * 2); // Finds all nested class declarations and doubles the number to count the curly braces belonging to it (which are missed by finding statements)
            statementCounter += cd.findAll(FieldDeclaration.class).size(); // Finds all field declarations outside of methods within the class 'cd' and all nested classes
            statementCounter += cd.findAll(MethodDeclaration.class).size(); // Finds all method declarations within the class 'cd' and all nested classes
            statementCounter += cd.findAll(ConstructorDeclaration.class).size(); // Finds all constructor declarations
            statementCounter += cd.findAll(EnumDeclaration.class).size(); // Finds all Enum declarations

            if(statementCounter > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many statements!\n");
            }

            super.visit(cd, arg);
        }

    }
}
