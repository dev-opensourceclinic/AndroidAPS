package app.aaps.core.interfaces.insulin

import app.aaps.core.data.insulin.InsulinType
import app.aaps.core.data.model.ICfg
import app.aaps.core.interfaces.configuration.ConfigExportImport

interface Insulin : ConfigExportImport {

    val id: InsulinType
    val friendlyName: String
    val comment: String
    val dia: Double
    val peak: Int

    //fun iobCalcForTreatment(bolus: BS, time: Long, dia: Double): Iob

    val iCfg: ICfg
}