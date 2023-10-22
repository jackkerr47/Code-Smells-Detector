import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class badSmells {

    public static void main(String[] args) throws Exception {
        //Enter the file you wish to perform static analysis upon
        FileInputStream in = new FileInputStream("Grid.java");

        //This returns the name of all java file names within the badSmells directory, to help detect feature envy within the above file
        Path directoryPath = Path.of("C:\\Users\\Ander\\IdeaProjects\\Software-Architecture-Project\\badSmells"); // Replace with the actual directory path

        ClassCollectorVisitor cc = new ClassCollectorVisitor();
        MessageChainVisitor mc = new MessageChainVisitor();
        TemporaryFieldVisitor fv = new TemporaryFieldVisitor();

        List<String> classList = new ArrayList<>();

        try {
            Stream<Path> pathStream = Files.walk(directoryPath, Integer.MAX_VALUE);
            Stream<Path> javaFiles = pathStream.filter(path -> path.toString().toLowerCase().endsWith(".java"));

            // Process each file and directory in the stream
            javaFiles.forEach(path -> {
                CompilationUnit cuStream;

                try {
                    cuStream = StaticJavaParser.parse(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                cc.visit(cuStream,null);

                // You can perform actions on each path here
            });

            // Close the stream when you're done
            pathStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        classList = cc.getClassList(); // This is a list of classes defined within all the Java files inside the project folder specified in 'directoryPath' - it is used to check for feature envy
        MethodAndClassDeclarationVisitor md = new MethodAndClassDeclarationVisitor(classList);
        md.visit(cu, null);
        mc.visit(cu, null);
        fv.visit(cu, null);

    }

    public static class ClassCollectorVisitor extends VoidVisitorAdapter {
        public List<String> classList = new ArrayList<>();

        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {
            for(ClassOrInterfaceDeclaration c : cd.findAll(ClassOrInterfaceDeclaration.class)){
                classList.add(c.getNameAsString());
            }
        }

        public List<String> getClassList(){
            return classList;
        }
    }

    // Temporary Fields (Medium - Detects the presence of temporary fields in the class level)
    public static class TemporaryFieldVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(FieldDeclaration fd, Object arg) {

            List<String> fieldNamesList = fd.getVariables().stream()
                    .map(var -> var.getName().asString())
                    .collect(Collectors.toCollection(ArrayList::new));

            for(String fieldName : fieldNamesList) {
                if (!isFieldUsedWithinMethods(fd, fieldName)) {
                    System.out.println("Warning: Temporary Field Detected: " + fieldName + "\n");
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
                System.out.println("Warning! Message Chain Detected: " + call.getScope().get() + " -> " + call.getName() + "\n");
            }
            super.visit(call, arg);
        }
    }

    private static class MethodAndClassDeclarationVisitor extends VoidVisitorAdapter {

        List<String> classList;

        private MethodAndClassDeclarationVisitor(List<String> classList){
            this.classList = classList;
        }

        @Override
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


            // Data Class (Medium)
            // A data class is defined as a class which has variables local to the class, a constructor, getter methods or setter methods - anything else and it is NOT a data class.

            // Below is an example of a getter method -

//            int variable;
//
//            public void getVariable(){
//                return variable; // returns a variable and does not modify it in any way
//            }


            // Below is an example of a setter method -

//            int variable;
//
//            public void setVariable(int variable){
//                this.variable = variable; // No modifications are allowed to the method parameter variable, 'this.' is optional if the method parameter and class variable do not have the same name
//            }

            Boolean isDataClass = true;
            for(Statement s : cd.findAll(Statement.class)){
                if(s instanceof ReturnStmt) {
                    if(s.asReturnStmt().getExpression().get() instanceof NameExpr){
                        // Do nothing as this is a valid return statement for a data class
                    } else{
                        isDataClass = false; // Cannot be data class as the return statement does not simply return a variable
                    }
                } else if (s instanceof BlockStmt) {
                    // Do nothing as a block statement in this case only consists of curly braces
                } else if(s instanceof ExpressionStmt){
                    Expression expression = s.asExpressionStmt().getExpression();
                    if(expression.isAssignExpr()
                            && ((expression.asAssignExpr().getTarget().isFieldAccessExpr() && fieldsDeclared(cd, expression.asAssignExpr().getTarget().asFieldAccessExpr().getNameAsString() ) && expression.asAssignExpr().getTarget().asFieldAccessExpr().getScope().isThisExpr())
                            || (expression.asAssignExpr().getTarget().isNameExpr() && fieldsDeclared(cd, expression.asAssignExpr().getTarget().asNameExpr().getNameAsString())))
                            && expression.asAssignExpr().getValue().isNameExpr()
                            && checkParameter(s)){
                        // Do nothing as this Expression Statement belongs in a data class

                        // In the following explanation of this if statement, it uses the above example setter method
                        // The above if statement checks a statement belongs to a setter method by checking the expression 'this.variable = variable;' is an Assign Expression,
                        // then checks the 'this.variable' is either a Field Access Expression or Name Expression (in this case it would just be 'variable') and checks the field 'variable' is a field declared in the class.
                        // If the expression is a Field Access Expression then it also checks it contains 'this.'
                        // It then checks the 'variable' on the right of the equals sign is a Name Expression (only a variable) and then checks it is a parameter of that method or constructor.

                    } else {
                        isDataClass = false; // If there is an Expression Statement that does not belong in a data class, then this class is not a data class
                    }
                } else {
                    isDataClass = false; // If there is a statement that is neither a return statement, block statement (curly braces) or an expression statement then this is not a data class as a data class only includes getter and setter methods
                }
            }

            if(isDataClass == true) {
                System.out.println("Warning: Class " + cd.getNameAsString() + " is a Data Class!\n");
            }


            // Middle Man (middle man class is defined as one which the method calls used all belong to other classes and no functionality is provided)
            boolean middleMan = true;
            if(cd.findAll(MethodDeclaration.class).size() == 0) {
                for (ConstructorDeclaration c : cd.findAll(ConstructorDeclaration.class)) { // If the class creates an object with a constructor and has no methods then this is not a middle man class
                    for (Statement s : c.getBody().findAll(Statement.class))
                        middleMan = middleManCheck(s, middleMan);
                }
            } else {
                for(MethodDeclaration md : cd.findAll(MethodDeclaration.class)){
                    for(Statement s : md.getBody().get().findAll(Statement.class)){
                        middleMan = middleManCheck(s, middleMan);
                    }
                }
            }
            if(middleMan == true) // If middleMan is true then the class must be a middle man since there is not statement that adds functionality to the class
                System.out.println("Warning: class " + cd.getNameAsString() + " is a middle man class!\n");


            // Feature Envy (Hard) - this is defined as when a class calls more external methods than internal
            Map<String, String> classVariables = new HashMap<>();  // Classes defined within the 'directoryPath' folder that could be used for feature envy mapped to the variable name of these data types
            for(VariableDeclarator vd : cd.findAll(VariableDeclarator.class)){
                if(classList.contains(vd.getTypeAsString())){
                    classVariables.put(vd.getNameAsString(), vd.getTypeAsString());
                }
            }
            for(MethodDeclaration md : cd.findAll(MethodDeclaration.class)){
                for(Parameter p : md.getParameters()){
                    if(classList.contains(p.getTypeAsString())){
                        classVariables.put(p.getNameAsString(), p.getTypeAsString());
                    }
                }
            }

            int thisClassMethodCalls = 0;
            int externalClassMethodCalls = 0;

            for(MethodCallExpr e : cd.findAll(MethodCallExpr.class)) {
                if(e.hasScope() && classVariables.containsKey(e.getScope().get().toString())){
                    externalClassMethodCalls++; // If the method call is on a variable that's data type is a class from 'directoryPath' then that is an external method call
                } else {
                    thisClassMethodCalls++; // Otherwise the method call is calling an internal method or a method of a standard Java data type
                }
            }

            if(externalClassMethodCalls > thisClassMethodCalls)
                System.out.println("Warning: class " + cd.getNameAsString() + " contains feature envy!\n");

            super.visit(cd, arg);
        }

        // Checks the string 'fieldName' is a field declared in the 'cd' class
        public boolean fieldsDeclared(ClassOrInterfaceDeclaration cd, String fieldName){
            for(FieldDeclaration fd : cd.findAll(FieldDeclaration.class)) {
                for (VariableDeclarator v : fd.getVariables()){
                    if (v.toString().equals(fieldName)) {
                        return true; // If fieldName is a variable declared in the class (outside of methods) then 'cd' can be a data class
                    }
                }
            }
            return false; // If fieldName is not a variable declared in the class (outside of methods then 'cd' cannot be a data class
        }

        // Checks the Statement 's' is a parameter in either a constructor or a method
        public boolean checkParameter(Statement s){
            Expression expression = s.asExpressionStmt().getExpression();
            String parameter = expression.asAssignExpr().getValue().asNameExpr().getNameAsString();
            if(s.findAncestor(MethodDeclaration.class).isPresent()){
                for(Parameter p : s.findAncestor(MethodDeclaration.class).get().getParameters()){
                    if(p.getName().asString().equals(parameter)){
                        return true; // If the parameters in the statement's method include the name expression from the statement then this can be a data class
                    }
                }
            } else if(s.findAncestor(ConstructorDeclaration.class).isPresent()){
                for(Parameter p : s.findAncestor(ConstructorDeclaration.class).get().getParameters()){
                    if(p.getName().asString().equals(parameter)){
                        return true; // If the parameters in the statement's constructor include the name expression from the statement then this can be a data class
                    }
                }
            }
            return false; // If the parameter is not in the statement's method or constructor then it cannot be a data class
        }

        public boolean middleManCheck(Statement s, boolean middleMan){
            if(s instanceof ExpressionStmt){
                if (s.asExpressionStmt().getExpression().isMethodCallExpr()) {
                    MethodCallExpr mc = (MethodCallExpr) s.asExpressionStmt().getExpression();
                    if (mc.getScope().isEmpty()) {
                        middleMan = false; // If the method call does not use another class's method then the class 'cd' is not a middle man class
                    }
                    for(Expression e : mc.getArguments()){
                        if(!e.isMethodCallExpr())
                            middleMan = false; // If the statement is not a method call then the class cannot be a middle man
                        else{
                            if(e.asMethodCallExpr().getScope().isEmpty())
                                middleMan = false; // If the method call does not use another class's method then the class 'cd' is not a middle man class
                        }
                    }
                } else
                    middleMan = false; // If the statement is not a method call then the class cannot be a middle man
            } else if(s instanceof LocalClassDeclarationStmt){
                middleMan = false; // If the class has a nested class then it is not a middle man
            } else if(s instanceof ReturnStmt){
                if (s.asReturnStmt().getExpression().isPresent()) {
                    if(s.asReturnStmt().getExpression().get().isMethodCallExpr()) {
                        MethodCallExpr mc = (MethodCallExpr) s.asReturnStmt().getExpression().get();
                        if (mc.getScope().isEmpty()) {
                            middleMan = false; // If the return statement does not use another class's method then the class 'cd' is not a middle man class
                        }
                    } else if(s.asReturnStmt().getExpression().get().isNameExpr()){
                        middleMan = false; // If it returns something other than a variable call then this is not a middle man class
                    }
                }
            }
            return middleMan;
        }

    }
}