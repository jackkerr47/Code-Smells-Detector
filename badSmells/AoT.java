public class AoT {  // Public class named aoT
    double a;  // Public double named a

    public void calcA(int b, int h, int c, int d, int e, int f){  // Method header - named calcA with the parameters
        a = (b*h)/2.0;  // Calculation in method body
    }

    public double getA(){  // Method header - named getA
        return a;  // Method body - return a
    }

    public static class TestClass {
        int a;
    }
}