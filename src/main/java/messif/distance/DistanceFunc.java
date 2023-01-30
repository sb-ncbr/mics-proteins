/*
 *  This file is part of MESSIF library: https://bitbucket.org/disalab/messif
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.distance;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Interface for distance functions. The distance function takes two parameters of type
 * {@code T} and returns their distance as a float number.
 *
 * @param <T> the type of the distance function arguments
 *
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface DistanceFunc<T> extends Serializable {

    // region ******************       Static constants      ******************

    /**
     * Logger to be used in the distance package.
     */
    Logger log = Logger.getLogger("distance function");

    /**
     * Unknown distance constant
     */
    float UNKNOWN_DISTANCE = Float.NEGATIVE_INFINITY;
    /**
     * Minimal possible distance constant
     */
    float MIN_DISTANCE = 0.0f;
    /**
     * Maximal possible distance constant
     */
    float MAX_DISTANCE = Float.MAX_VALUE;
    // endregion


    /**
     * Returns the distance between object {@code o1} and object {@code o2}.
     *
     * @param o1 the object for which to measure the distance
     * @param o2 the object for which to measure the distance
     * @return the distance between object {@code o1} and object {@code o2}
     */
    default float getDistance(T o1, T o2) {
        return getDistance(o1, o2, MAX_DISTANCE);
    }

    /**
     * Returns the distance between object {@code o1} and object {@code o2}.
     *
     * @param o1 the object for which to measure the distance
     * @param o2 the object for which to measure the distance
     * @return the distance between object {@code o1} and object {@code o2}
     */
    float getDistance(T o1, T o2, float threshold);

    /**
     * Returns the maximal distance this distance function can return (default is {@link #MAX_DISTANCE}).
     * @return maximal distance this distance function can return
     */
    default float getMaxDistance() {
        return MAX_DISTANCE;
    }

    /**
     * Return the class of the expected objects.
     */
    Class<T> getObjectClass();

}
