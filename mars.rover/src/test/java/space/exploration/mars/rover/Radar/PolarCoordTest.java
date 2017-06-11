package space.exploration.mars.rover.Radar;

import space.exploration.mars.rover.utils.RadialContact;

import java.awt.*;
import java.util.*;

/**
 * Created by sanket on 6/10/17.
 */
public class PolarCoordTest {

    public static void main(String[] args) {
        Point a = new Point(0, 0);
        Point b = new Point(45, 45);
        Point c = new Point(100, 100);

        RadialContact bR = new RadialContact(a, b);
        RadialContact cR = new RadialContact(a, c);

        System.out.println("bR center = " + bR.getCenter().toString() + " contact= " + bR.getContactPoint().toString()
                                   + " bR polarPoint = " + bR.getPolarPoint().toString()
        );

        System.out.println(" Angle = " + Math.toDegrees(bR.getPolarPoint().getTheta()));

        System.out.println("cR center = " + cR.getCenter().toString() + " contact= " + cR.getContactPoint().toString()
                                   + " cR polarPoint = " + cR.getPolarPoint().toString()
        );

        System.out.println(" Angle = " + Math.toDegrees(cR.getPolarPoint().getTheta()));

        if (bR.compareTo(cR) > 0) {
            System.out.println(" b > c");
        } else {
            System.out.println(" c > b");
        }

        java.util.List<RadialContact> list = new ArrayList<>();
        list.add(bR);
        list.add(cR);

        Collections.sort(list);

        for (RadialContact r : list) {
            System.out.println(r.getPolarPoint().getR() + " " + r.getPolarPoint().getTheta());
        }

    }
}
