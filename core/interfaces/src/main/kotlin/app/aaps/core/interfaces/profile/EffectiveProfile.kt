package app.aaps.core.interfaces.profile

import app.aaps.core.data.model.ICfg

/**
 * Profile with applied insulin configuration (Effective profile)
 */
interface EffectiveProfile : Profile {

    /** Applied insulin configuration */
    val iCfg: ICfg
}