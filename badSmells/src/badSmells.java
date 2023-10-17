import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;


public class badSmells {

    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("Grid.java");

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        MethodDeclarationVisitor md = new MethodDeclarationVisitor();
        ClassOrInterfaceDeclarationVisitor cd = new ClassOrInterfaceDeclarationVisitor();
        MessageChainVisitor mc = new MessageChainVisitor();
        TemporaryFieldVisitor fv = new TemporaryFieldVisitor();

        md.visit(cu, null);
        cd.visit(cu, null);
        mc.visit(cu, null);
        fv.visit(cu, null);
    }

    public static class TemporaryFieldVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(FieldDeclaration fd, Object arg) {

            List<String> fieldNamesList = fd.getVariables().stream()
                    .map(var -> var.getName().asString())
                    .collect(Collectors.toCollection(ArrayList::new));

            for(String fieldName : fieldNamesList) {
                if (!isFieldUsedWithinMethods(fd, fieldName)) {
                    System.out.println("Temporary Field Detected: " + fieldName);
                }
            }
            super.visit(fd, arg);
        }

        private boolean isFieldUsedWithinMethods(FieldDeclaration fd, String fieldName) {

            //need to get cu as the methods in the whole AST will be out of scope here
            Optional<CompilationUnit> cu = fd.findCompilationUnit();
            List<MethodDeclaration> methodDeclarations = new ArrayList<>();

            if (cu.isPresent()) {
                methodDeclarations = cu.get().findAll(MethodDeclaration.class);
            } else {
                System.out.println("CompilationUnit not found.");
            }

            for (MethodDeclaration md : methodDeclarations) {
                List<MethodCallExpr> methodCallExpressions = new ArrayList<>();
                md.accept(new MethodCallVisitor(methodCallExpressions), null);

                for (MethodCallExpr methodCallExpr : methodCallExpressions) {
                    NodeList<Expression> arguments = methodCallExpr.getArguments();
                    for (Expression expression : arguments) {
                        if (expression.isNameExpr() && expression.asNameExpr().getName().asString().equals(fieldName)) {
                            return true; // Field is used within a method call argument
                        }
                    }
                }
            }
            return false; // Field is not used within method call arguments

        }

        private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
            private final List<MethodCallExpr> methodCallExpressions;

            public MethodCallVisitor(List<MethodCallExpr> methodCallExpressions) {
                this.methodCallExpressions = methodCallExpressions;
            }

            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                methodCallExpressions.add(n);
            }
        }
    }

    private static class MessageChainVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodCallExpr call, Object arg) {
            if (call.getScope().isPresent() && call.getScope().get() instanceof MethodCallExpr) {
                System.out.println("Warning! Message Chain Detected: " + call.getScope().get() + " -> " + call.getName());
            }
            super.visit(call, arg);
        }
    }

    private static class ClassOrInterfaceDeclarationVisitor extends VoidVisitorAdapter {
        @Override
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
    }

    private static class MethodDeclarationVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration md, Object arg) {
            BlockStmt body = md.getBody().get();

            // Long Parameter List
            if (md.getParameters().size() > 5) {
                System.out.println("Warning: long parameter list in method " + md.getName() + "!");
            }

            // Long Method (Easy: counting all lines of code - only including lines from the method body)
            if ((body.getEnd().get().line) - (body.getBegin().get().line) - 1 > 20) {
                System.out.println("(Easy) Warning: method " + md.getName() + " contains too many lines of code!");
            }

            // Long Method (Medium: counting statements - only including lines from the method body)
            if(md.findAll(Statement.class).size() - 1 > 20) {
                System.out.println("(Medium) Warning: method " + md.getName() + " contains too many lines of code!");
            }
                System.out.println("Method " + md.getName() + " contains " + (md.findAll(Statement.class).size() - 1) + " statements within it. \n");

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
