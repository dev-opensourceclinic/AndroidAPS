package app.aaps.core.interfaces.pump

interface PumpWithConcentration : Pump {
    /*
     * Extend interface here if you need to miss concentration modification i.e. Filling bolus
     */

    /**
     * Return real pump (selected in ConfigBuilder) class
     * ie. VirtualPumpPlugin::class.java
     */
    fun activePumpInternalClass(): Class<out Pump>
}