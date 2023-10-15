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

            int statementCounter = md.findAll(Statement.class).size();
//            for(Statement s : md.findAll(Statement.class)){
//                System.out.println("Statement for method " + md.getNameAsString() + " - " + s);
//            }

            System.out.println("Final statement counter = " + statementCounter + " for method " + md.getName());
            super.visit(md, arg);
        }

        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {

            // Large Class (Easy: counting all lines of code - only including lines from the method body)
            if((cd.getEnd().get().line) - (cd.getBegin().get().line) - 1 > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many lines");
            }

            int statementCounter = cd.findAll(Statement.class).size();
//            for(Statement s : cd.findAll(Statement.class)){
//                System.out.println("Statement for class " + cd.getNameAsString() + " - " + s);
//            }
            System.out.println("statement counter = " + statementCounter + " - in class " + cd.getNameAsString());

            super.visit(cd, arg);
        }
    }
}
