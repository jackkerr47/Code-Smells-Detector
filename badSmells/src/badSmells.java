import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.FileInputStream;
import java.lang.reflect.Method;
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
                if(s.getChildNodes().size() >= 1){
                    getChildNodes(s.getChildNodes(),statementCounter);
                }

                /*if(s.getChildNodes().size() > 1) {
                    statementCounter += s.getChildNodes().size();
                } else {
                    statementCounter++;
                }*/
                /*System.out.println("Statement : " + s);
                if (s.isWhileStmt() || s.isIfStmt()) {

                    System.out.println("accept called");
                    s.accept(this, null);
                }*/
                //statementCounter = statementVisitor(s,statementCounter);
            }
            System.out.println("Final statement counter = " + statementCounter + " for method " + md.getName());
            super.visit(md, arg);
        }

        public int getChildNodes(List<Node> nodeList, int statementCounter) {
            for(Node n : nodeList){
                statementCounter += nodeList.size();
                if(n.getChildNodes().size() > 1){
                    getChildNodes(n.getChildNodes(),statementCounter);
                }
            }
            return statementCounter;
        }


        /*public int statementVisitor(Statement s, int statementCounter) {

            statementCounter++;

            /*for(Node n : s.getChildNodes()) {
                System.out.println(n + "-CHILD-");
                System.out.println(n.getChildNodes() + "-CHILDREN-");
                for(Node m : n.getChildNodes()){
                    if(m.getChildNodes().size() == 1){
                        System.out.println("Child node is " + m);
                    }else{
                        System.out.println("Child node has more and is " + m);
                        System.out.println(m.getChildNodes());
                    }
                }
            }


            if (s.isWhileStmt() || s.isIfStmt()) {
                System.out.println("accept called");
            }
            return statementCounter;
        }*/

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
                    System.out.println("hdbbfjdnf");
                    //member.accept(this,null);

                    //bd.remove(); // NEED TO REMOVE THE CLASS HEADER AND CLOSING CURLY BRACKET HERE BEFORE PASSING CLASS BACK IN TO PREVENT INFINTE LOOP!!!!!!!
                    //counter += totalClassSize(bd, counter);
                }
            }

            System.out.println("statement counter = " + statementCounter + " - in class " + cd.getNameAsString());

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
