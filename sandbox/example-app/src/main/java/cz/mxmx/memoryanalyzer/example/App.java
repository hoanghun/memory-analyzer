package cz.mxmx.memoryanalyzer.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Child {
    String childName;
    Parent parent;

    public Child(String childName, Parent parent) {
        this.childName = childName;
        this.parent = parent;
    }
}

class Parent {
    String parentName;

    public Parent(String parentName) {
        this.parentName = parentName;
    }
}

class PrimitiveField {
    long number;

    public PrimitiveField(long number) {
        this.number = number;
    }
}


public class App {
	public static void main(String[] args) {
//	    shallowComparison();
//        deepComparison();
        primitiveField();
    }

    public static void shallowComparison() {
	    int instanceDuplicateCount = 10;
	    int totalCount = 10;
        List<Child> children = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            Parent parent = new Parent("Parent " + i);
            for (int j = 0; j < instanceDuplicateCount; j++) {
                children.add(new Child("Child " + i, parent));
            }
        }

        System.out.println("press something");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deepComparison() {
        int instanceDuplicateCount = 10;
        int totalCount = 10;
        List<Child> children = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            for (int j = 0; j < instanceDuplicateCount; j++) {
                Parent parent = new Parent("Parent " + i);
                children.add(new Child("Child " + i, parent));
            }
        }

        System.out.println("press something");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void primitiveField() {
	   List<PrimitiveField> primitiveFields = new ArrayList<>();

	   for (int i = 0; i < 100; i++) {
	       primitiveFields.add(new PrimitiveField(10));
       }

        System.out.println("press something");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
