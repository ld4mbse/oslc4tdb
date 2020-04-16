package com.ld4mbse.oslc4tdb.repackage;

public class CheckingPackageName {

    public static void main(String[] args) {

        try {
            System.out.println("=================");
            System.out.println();
            System.out.println();
            System.out.println("Checking Refactor name for Transformer module:");
            Class transformer = Class.forName("com.ld4mbse.oslc4tdb.transformer.Transformer");
            System.out.println("Checkin Transformer class:");
            System.out.println("Package: " + transformer.getName());
            System.out.println("Package startsWith(\"com.ld4mbse\"): " + transformer.getName().startsWith("com.ld4mbse"));
            System.out.println("=================");
            System.out.println();
            System.out.println();
            System.out.println("Checking Refactor name for webapp module:");
            Class tdbStoreResource = Class.forName("com.ld4mbse.oslc4tdb.rest.TDBStoreResource");
            System.out.println("Checkin TDBStoreResource class:");
            System.out.println("Package: " + tdbStoreResource.getName());
            System.out.println("Package startsWith(\"com.ld4mbse\"): " + tdbStoreResource.getName().startsWith("com.ld4mbse"));

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
