import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import javax.sound.sampled.Line;
import javax.swing.plaf.nimbus.State;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class badSmells {

    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("AoT.java");

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        new MethodDeclarationVisitor().visit(cu, null);
    }


    private static class MethodDeclarationVisitor extends VoidVisitorAdapter {

        public void visit(MethodDeclaration md, Object arg) {
            BlockStmt body = md.getBody().get();

            // Long Parameter List
            if (md.getParameters().size() > 5) {
                System.out.println("Warning: long parameter list in method " + md.getName() + "!");
            }

            // Long Method (Easy: counting all lines of code - only including lines from the method body)
            if ((body.getEnd().get().line) - (body.getBegin().get().line) - 1 > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many lines of code!");
            }

            /*// Long Method (Medium: counting statements - only including lines from the method body)
            if(body.getStatements().size() > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many statements!");
            }*/

            //System.out.println(body.getStatements() + " <- statements");
            //System.out.println(body.getStatements().size())
            int statementCounter = 0;
            for (Statement s : body.getStatements()) {
                statementCounter++;
                if((s.isIfStmt() || s.isWhileStmt()) && s.getChildNodes().size() >= 1){
                    searchStatements(s, statementCounter);
                }
            }
            System.out.println("Final statement counter = " + statementCounter + " for method " + md.getName() + "\n");
            super.visit(md, arg);
        }

        public void searchStatements(Node statementContents, int statementCounter) {
//            if(statementContents.getChildNodes().get(1) != null) {
                Node statement = statementContents.getChildNodes().get(1);
                for (Node statementLine : statement.getChildNodes()) {
                    System.out.println(statementLine + " statement line");
                    statementCounter++;
                    if ((statementLine.findAncestor(IfStmt.class).isPresent() || statementLine.findAncestor(WhileStmt.class).isPresent())
                            && statementLine.getChildNodes().size() >= 1) {
                        searchStatements(statement, statementCounter);
                    }
                }
//            }
        }


        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {

            // Large Class (Easy: counting all lines of code - only including lines from the method body)
            if((cd.getEnd().get().line) - (cd.getBegin().get().line) - 1 > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many lines");
            }

            int statementCounter = 0;
            NodeList<BodyDeclaration<?>> members = cd.getMembers();

            for(BodyDeclaration member : members){
               //statementCounter += totalClassSize(member, statementCounter);
                if(member.isFieldDeclaration()) {
                    statementCounter++;
                }
                if(member.isMethodDeclaration()){
                    MethodDeclaration method = (MethodDeclaration) member;
                    statementCounter += method.getBody().get().getStatements().size() + 2;
                }
                if(member.isClassOrInterfaceDeclaration()) {
                    statementCounter +=2;
                    //member.accept(this,null);
                }
            }

            //System.out.println("statement counter = " + statementCounter + " - in class " + cd.getNameAsString());

            super.visit(cd, arg);
        }

        public int totalClassSize(BodyDeclaration bd, int statementCounter) {
            int counter = statementCounter;

            if(bd.isFieldDeclaration()) {
                counter++;
            }
            if(bd.isMethodDeclaration()){
                MethodDeclaration method = (MethodDeclaration) bd;
                counter += method.getBody().get().getStatements().size() + 2;
            }
            if(bd.isClassOrInterfaceDeclaration()) {
                bd.accept(this,null);
                //bd.remove(); // NEED TO REMOVE THE CLASS HEADER AND CLOSING CURLY BRACKET HERE BEFORE PASSING CLASS BACK IN TO PREVENT INFINTE LOOP!!!!!!!
                //counter += totalClassSize(bd, counter);
            }

            return counter;
        }



        /* public static int countLinesOfCode(ClassOrInterfaceDeclaration cd) {
            int validLineCount = 0;

            int startLine = cd.getBegin().get().line;
            int endLine = cd.getEnd().get().line;

            String[] lines = cd.getTokenRange().get().toString().split("\n");
            for(String line : lines){
                if(line.trim().length() > 0){
                    validLineCount++;
                }
            }

            System.out.println("Valid line count: " + validLineCount);
            return 0;
        } */
    }
}
