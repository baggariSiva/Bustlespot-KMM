
package org.softsuave.bustlespot.locationmodule

import dev.jordond.compass.geolocation.Locator

expect class MGeoLocator(){
    fun getLocator(): Locator
}