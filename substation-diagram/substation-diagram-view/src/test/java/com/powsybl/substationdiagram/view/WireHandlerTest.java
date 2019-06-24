package com.powsybl.substationdiagram.view;

import com.google.common.collect.ImmutableList;
import com.powsybl.substationdiagram.view.WireHandler.OrientedPosition;
import javafx.geometry.Point2D;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class WireHandlerTest {

    @Test
    public void computePointAtDistance() {

        List<Point2D> points = ImmutableList.of(new Point2D(1, 0),
                new Point2D(1, 1),
                new Point2D(2, 1),
                new Point2D(2, 2),
                new Point2D(1, 2));

        OrientedPosition middle1 = WireHandler.positionAtDistance(points, 0.5);
        assertEquals(new Point2D(1, 0.5), middle1.getPoint());
        assertEquals(0, middle1.getOrientation(), 0);

        OrientedPosition middle2 = WireHandler.positionAtDistance(points, 1.5);
        assertEquals(new Point2D(1.5, 1), middle2.getPoint());
        assertEquals(-90, middle2.getOrientation(), 0);

        OrientedPosition middle3 = WireHandler.positionAtDistance(points, 2.5);
        assertEquals(new Point2D(2, 1.5), middle3.getPoint());
        assertEquals(0, middle3.getOrientation(), 0);

        OrientedPosition middle4 = WireHandler.positionAtDistance(points, 3.5);
        assertEquals(new Point2D(1.5, 2), middle4.getPoint());
        assertEquals(90, middle4.getOrientation(), 0);
    }

}
