package c64.emulation.cpu


/**
 * Clock - helper to get the right timing handled. It runs with a resolution of 1 millisecond.
 *
 * @author Daniel Schulte 2017-2019
 */
class Clock {

    fun start(callback: (Long) -> Long) {
        var lastMilliSecond = System.currentTimeMillis()
        // clock main loop
        while (true) {
            // get current millisecond
            val currentMilliSecond = System.currentTimeMillis()
            // check whether milliseconds counted up
            if (currentMilliSecond > lastMilliSecond) {
                callback.invoke((currentMilliSecond - lastMilliSecond) * 1000)
                lastMilliSecond = currentMilliSecond
            }
            else {
                // same millisecond, sleep 1 millisecond instead
                Thread.sleep(1)
            }
        }
    }

    // OLD CODE - TRY TO RUN WITH A RESOLUTION OF MICROSECONDS
    /*fun start(callback: (Long) -> Long) {
        var lastMicroSecond = System.nanoTime() / 1_000
        // clock main loop
        while (true) {
            // get current microsecond
            val currentMicroSecond = System.nanoTime() / 1_000
            // check whether microseconds counted up
            if (currentMicroSecond > lastMicroSecond) {
                // run callback method for n cycles and add the additional cycles to lastMicroSecond
                lastMicroSecond = currentMicroSecond + callback.invoke(currentMicroSecond - lastMicroSecond)
            }
        }
    }*/
}