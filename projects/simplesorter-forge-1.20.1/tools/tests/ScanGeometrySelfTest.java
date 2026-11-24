package com.kirill.simplesorter.sorting;
public final class ScanGeometrySelfTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError("Radius geometry");}
    public static void main(String[] args){
        check(ScanGeometry.contains(17,0,0,32));
        check(ScanGeometry.contains(32,0,32,32));
        check(ScanGeometry.contains(-32,32,-32,32));
        check(!ScanGeometry.contains(33,0,0,32));
        check(!ScanGeometry.contains(0,-33,0,32));
        check(!ScanGeometry.contains(0,0,33,32));
        check(ScanGeometry.contains(0,0,0,8));
        System.out.println("Square radius: distant and boundary coordinates passed.");
    }
}
